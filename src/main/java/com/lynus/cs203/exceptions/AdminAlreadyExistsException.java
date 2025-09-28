package com.lynus.cs203.exceptions;

public class AdminAlreadyExistsException extends  RuntimeException {
    public AdminAlreadyExistsException(String message) {
        super(message);
    }
}
