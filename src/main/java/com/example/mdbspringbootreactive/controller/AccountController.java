package com.example.mdbspringbootreactive.controller;

import com.example.mdbspringbootreactive.entity.ResponseMessage;
import com.example.mdbspringbootreactive.entity.TransferRequest;
import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.exception.AccountNotFoundException;
import com.example.mdbspringbootreactive.exception.TransactionException;
import com.example.mdbspringbootreactive.model.Account;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.model.TxnEntry;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.service.TxnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class AccountController {

    private final static Logger LOGGER = LoggerFactory.getLogger(AccountController.class);
    private final AccountRepository accountRepository;
    private final TxnService txnService;

    public AccountController(AccountRepository accountRepository, TxnService txnService) {
        this.accountRepository = accountRepository;
        this.txnService = txnService;
    }

    private static void printLastLineStackTrace(String context) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        LOGGER.info("Stack trace's last line: " + stackTrace[stackTrace.length - 1].toString() + " from " + context);
    }

    @PostMapping("/account")
    public Mono<Account> createAccount(@RequestBody Account account) {
        printLastLineStackTrace("POST /account");
        return accountRepository.save(account);
    }

    @GetMapping("/account/{accountNum}")
    public Mono<Account> getAccount(@PathVariable String accountNum) {
        printLastLineStackTrace("GET /account/" + accountNum);
        return accountRepository.findByAccountNum(accountNum).switchIfEmpty(Mono.error(new AccountNotFoundException()));
    }

    @PostMapping("/account/{accountNum}/debit")
    public Mono<Txn> debitAccount(@PathVariable String accountNum, @RequestBody Map<String, Object> requestBody) {
        printLastLineStackTrace("POST /account/" + accountNum + "/debit");
        Txn txn = new Txn();
        double amount = ((Number) requestBody.get("amount")).doubleValue();
        txn.addEntry(new TxnEntry(accountNum, amount));
        return txnService.saveTransaction(txn).flatMap(txnService::executeTxn);
    }

    @PostMapping("/account/{accountNum}/credit")
    public Mono<Txn> creditAccount(@PathVariable String accountNum, @RequestBody Map<String, Object> requestBody) {
        printLastLineStackTrace("POST /account/" + accountNum + "/credit");
        Txn txn = new Txn();
        double amount = ((Number) requestBody.get("amount")).doubleValue();
        txn.addEntry(new TxnEntry(accountNum, -amount));
        return txnService.saveTransaction(txn).flatMap(txnService::executeTxn);
    }

    @PostMapping("/account/{from}/transfer")
    public Mono<Txn> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest) {
        printLastLineStackTrace("POST /account/" + from + "/transfer");
        String to = transferRequest.getTo();
        double amount = ((Number) transferRequest.getAmount()).doubleValue();
        Txn txn = new Txn();
        txn.addEntry(new TxnEntry(from, -amount));
        txn.addEntry(new TxnEntry(to, amount));
        //save pending transaction then execute
        return txnService.saveTransaction(txn).flatMap(txnService::executeTxn);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ResponseMessage> accountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.badRequest().body(new ResponseMessage(ErrorReason.ACCOUNT_NOT_FOUND.name()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ResponseMessage> duplicateAccount(DuplicateKeyException ex) {
        return ResponseEntity.badRequest().body(new ResponseMessage(ErrorReason.DUPLICATE_ACCOUNT.name()));
    }

    @ExceptionHandler(TransactionException.class)
    ResponseEntity<Mono<Txn>> insufficientBalance(TransactionException ex) {
        return ResponseEntity.unprocessableEntity().body(txnService.saveTransaction(ex.getTxn()));
    }

}
