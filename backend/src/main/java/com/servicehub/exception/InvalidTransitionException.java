package com.servicehub.exception;

public class InvalidTransitionException extends RuntimeException {
    
    public InvalidTransitionException(String message) {
        super(message);
    }
    
    public InvalidTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
