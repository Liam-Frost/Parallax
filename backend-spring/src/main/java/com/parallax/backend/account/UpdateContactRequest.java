package com.parallax.backend.account;

public record UpdateContactRequest(String email, String phoneCountry, String phone, String currentPassword) {
}
