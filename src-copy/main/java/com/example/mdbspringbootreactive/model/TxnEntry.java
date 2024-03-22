package com.example.mdbspringbootreactive.model;

public class TxnEntry {
    private String accountNum;
    private double amount;

    public TxnEntry(String accountNum, double amount) {
        this.accountNum = accountNum;
        this.amount = amount;
    }

    public String getAccountNum() {
        return accountNum;
    }

    public void setAccountNum(String accountNum) {
        this.accountNum = accountNum;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
