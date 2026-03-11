package com.servicehub.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> buildBody(HttpStatus status, String error, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<Object> handleInvalidTransition(
            InvalidTransitionException ex, WebRequest request) {

        return new ResponseEntity<>(
                buildBody(
                        HttpStatus.BAD_REQUEST,
                        "Invalid Status Transition",
                        ex.getMessage(),
                        request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        return new ResponseEntity<>(
                buildBody(
                        HttpStatus.NOT_FOUND,
                        "Resource Not Found",
                        ex.getMessage(),
                        request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SlaPolicyNotFoundException.class)
    public ResponseEntity<Object> handleSlaPolicyNotFound(
            SlaPolicyNotFoundException ex, WebRequest request) {

        return new ResponseEntity<>(
                buildBody(
                        HttpStatus.NOT_FOUND,
                        "SLA Policy Not Found",
                        ex.getMessage(),
                        request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(
            Exception ex, WebRequest request) {

        return new ResponseEntity<>(
                buildBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        ex.getMessage(),
                        request),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}