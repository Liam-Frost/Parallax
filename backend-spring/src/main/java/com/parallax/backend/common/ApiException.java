package com.parallax.backend.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
