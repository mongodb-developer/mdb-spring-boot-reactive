package com.example.mdbspringbootreactive.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document("accounts")
public class Account {

    private String accountNum;
    private double balance;

    public Account(String accountNum, double balance) {
        super();
        this.accountNum = accountNum;
        this.balance = balance;
    }

    public String getAccountNum() {
        return accountNum;
    }

    public void setAccountNum(String accountNum) {
        this.accountNum = accountNum;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

}
