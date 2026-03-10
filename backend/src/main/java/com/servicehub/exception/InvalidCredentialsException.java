package com.servicehub.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the provided email/password combination is wrong. */
public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid email or password. Please check your credentials and try again.", HttpStatus.UNAUTHORIZED);
    }
}

