package com.servicehub.exception;

public class SlaPolicyNotFoundException extends RuntimeException {
    
    public SlaPolicyNotFoundException(String message) {
        super(message);
    }
    
    public SlaPolicyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
