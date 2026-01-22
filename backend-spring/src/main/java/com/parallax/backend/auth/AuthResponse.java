package com.parallax.backend.auth;

public record AuthResponse(boolean success, String accessToken, long expiresIn, AuthUserView user) {
}
