package com.servicehub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * Exception raised for invalid transition exception.
 */

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTransitionException extends RuntimeException {
    
    public InvalidTransitionException(String message) {
        super(message);
    }
}
