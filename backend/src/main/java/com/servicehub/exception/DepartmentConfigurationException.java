package com.servicehub.exception;

import org.springframework.http.HttpStatus;

public class DepartmentConfigurationException extends AuthException {

    public DepartmentConfigurationException(String departmentName) {
        super("Department is not configured: " + departmentName, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
