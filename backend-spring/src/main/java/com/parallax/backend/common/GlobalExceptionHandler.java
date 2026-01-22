package com.parallax.backend.common;

import com.parallax.backend.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        if ("EMAIL_EXISTS".equals(ex.getErrorCode()) || "PHONE_EXISTS".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        } else if ("INVALID_REQUEST".equals(ex.getErrorCode())) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "INTERNAL_ERROR";
        String message = "Unexpected error";
        if (ex instanceof IllegalStateException) {
            status = HttpStatus.UNAUTHORIZED;
            code = "UNAUTHORIZED";
            message = ex.getMessage();
        }
        return ResponseEntity.status(status).body(ApiError.of(code, message));
    }
}
