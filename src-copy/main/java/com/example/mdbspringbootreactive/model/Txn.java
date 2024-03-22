package com.example.mdbspringbootreactive.model;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.enumeration.TxnStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("transactions")
public class Txn {

    @Id
    private String id;
    private List<TxnEntry> entries;

    private TxnStatus status;

    private LocalDateTime transactionDate;

    private ErrorReason errorReason;

    public Txn(List<TxnEntry> entries, TxnStatus status, ErrorReason errorReason, LocalDateTime transactionDate) {
        this.entries = entries;
        this.status = status;
        this.errorReason = errorReason;
        this.transactionDate = transactionDate;
    }

    public Txn() {
        this.entries = new ArrayList<>();
        this.status = TxnStatus.PENDING;
        this.transactionDate = LocalDateTime.now();
    }

    public void addEntry(TxnEntry entry) {
        entries.add(entry);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TxnEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<TxnEntry> entries) {
        this.entries = entries;
    }

    public TxnStatus getStatus() {
        return status;
    }

    public void setStatus(TxnStatus status) {
        this.status = status;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(ErrorReason errorReason) {
        this.errorReason = errorReason;
    }
}
