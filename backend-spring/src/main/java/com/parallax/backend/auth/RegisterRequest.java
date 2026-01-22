package com.parallax.backend.auth;

public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String country,
        Integer birthYear,
        Integer birthMonth,
        Integer birthDay,
        String phoneCountry,
        String phone,
        String contactMethod
) {
}
