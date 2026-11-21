package com.installment.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String field;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.field = null;
    }

    public AppException(HttpStatus status, String message, String field) {
        super(message);
        this.status = status;
        this.field = field;
    }

    public HttpStatus getStatus() { return status; }
    public String getField() { return field; }
}

