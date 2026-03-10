package com.servicehub.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a registration attempt uses an email that is already taken. */
public class EmailAlreadyExistsException extends AuthException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists.", HttpStatus.CONFLICT);
    }
}

