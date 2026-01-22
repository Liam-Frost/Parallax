package com.parallax.backend.account;

public record UpdatePasswordRequest(String oldPassword, String newPassword, String confirmPassword, String captcha) {
}
