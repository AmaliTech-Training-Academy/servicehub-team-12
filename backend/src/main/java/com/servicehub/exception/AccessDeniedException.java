package com.servicehub.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends AuthException {

    public AccessDeniedException() {
        super("You do not have permission to access this resource.", HttpStatus.FORBIDDEN);
    }
}

