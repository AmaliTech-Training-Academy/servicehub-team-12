package com.servicehub.exception;

import org.springframework.http.HttpStatus;
/**
 * Exception raised for resource not found exception.
 */

public class ResourceNotFoundException extends AuthException {

    public ResourceNotFoundException(String resource) {
        super(resource + " not found.", HttpStatus.NOT_FOUND);
    }
}
