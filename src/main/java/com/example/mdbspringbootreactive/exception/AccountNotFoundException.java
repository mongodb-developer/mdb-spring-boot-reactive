package com.example.mdbspringbootreactive.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("Account Not Found");
    }
}
