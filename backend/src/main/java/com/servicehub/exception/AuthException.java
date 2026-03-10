package com.servicehub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all authentication-related exceptions.
 * Carries an HTTP status so the global handler can respond correctly.
 */
@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}

