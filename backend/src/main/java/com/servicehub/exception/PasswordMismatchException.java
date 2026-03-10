package com.servicehub.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a password confirmation does not match the password field. */
public class PasswordMismatchException extends AuthException {

    public PasswordMismatchException() {
        super("Password and confirmation password do not match.", HttpStatus.BAD_REQUEST);
    }
}

