package com.example.mdbspringbootreactive.service;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.enumeration.TxnStatus;
import com.example.mdbspringbootreactive.exception.AccountNotFoundException;
import com.example.mdbspringbootreactive.exception.TransactionException;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.template.TxnTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TxnService {
    private final TxnTemplate txnTemplate;
    private final AccountRepository accountRepository;
    /**
     * Using Transactional Operator to manage transactions
     */
    private final TransactionalOperator transactionalOperator;


    TxnService(TxnTemplate txnTemplate,
               AccountRepository accountRepository,
               TransactionalOperator transactionalOperator) {
        this.txnTemplate = txnTemplate;
        this.accountRepository = accountRepository;
        this.transactionalOperator = transactionalOperator;
    }

    /**
     * Using @Transactional annotation to manage transactions
     */
    /*@Transactional
    public Mono<Txn> executeTxn(Txn txn) {
        return updateBalances(txn).onErrorResume(DataIntegrityViolationException.class, e -> {
            txn.setStatus(TxnStatus.FAILED);
            txn.setErrorReason(ErrorReason.INSUFFICIENT_BALANCE);
            return Mono.error(new TransactionException(txn));
        }).onErrorResume(AccountNotFoundException.class, e -> {
            txn.setStatus(TxnStatus.FAILED);
            txn.setErrorReason(ErrorReason.ACCOUNT_NOT_FOUND);
            return Mono.error(new TransactionException(txn));
        }).then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS));
    }*/
    public Mono<Txn> saveTransaction(Txn txn) {
        return txnTemplate.save(txn);
    }

    public Mono<Txn> executeTxn(Txn txn) {
        return updateBalances(txn).onErrorResume(DataIntegrityViolationException.class, e -> {
                                      txn.setStatus(TxnStatus.FAILED);
                                      txn.setErrorReason(ErrorReason.INSUFFICIENT_BALANCE);
                                      return Mono.error(new TransactionException(txn));
                                  })
                                  .onErrorResume(AccountNotFoundException.class, e -> {
                                      txn.setStatus(TxnStatus.FAILED);
                                      txn.setErrorReason(ErrorReason.ACCOUNT_NOT_FOUND);
                                      return Mono.error(new TransactionException(txn));
                                  })
                                  .then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS))
                                  .as(transactionalOperator::transactional);
    }

    public Flux<Long> updateBalances(Txn txn) {
        //read entries to update balances, concatMap maintains the sequence
        Flux<Long> updatedCounts = Flux.fromIterable(txn.getEntries())
                                       .concatMap(entry -> accountRepository.findAndIncrementBalanceByAccountNum(
                                               entry.getAccountNum(), entry.getAmount()));
        return updatedCounts.handle((updatedCount, sink) -> {
            if (updatedCount < 1) {
                sink.error(new AccountNotFoundException());
            } else {
                sink.next(updatedCount);
            }
        });
    }

}
