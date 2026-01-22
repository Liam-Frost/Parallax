package com.parallax.backend.auth;

public record AuthResult(String accessToken, String refreshToken, AuthUserView user) {
}
