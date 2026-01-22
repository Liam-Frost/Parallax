package com.parallax.backend.common;

public record ApiError(boolean success, String errorCode, String message) {
    public static ApiError of(String code, String message) {
        return new ApiError(false, code, message);
    }
}
