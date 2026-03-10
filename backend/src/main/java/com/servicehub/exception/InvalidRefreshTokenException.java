package com.servicehub.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a refresh token is invalid, not found, or already used. */
public class InvalidRefreshTokenException extends AuthException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token. Please log in again.", HttpStatus.UNAUTHORIZED);
    }
}

