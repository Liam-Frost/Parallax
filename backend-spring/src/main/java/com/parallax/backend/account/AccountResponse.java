package com.parallax.backend.account;

import com.parallax.backend.user.UserEntity;

public record AccountResponse(
        String email,
        String displayName,
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
    public static AccountResponse from(UserEntity user) {
        return new AccountResponse(
                user.getEmail(),
                user.getDisplayName(),
                user.getFirstName(),
                user.getLastName(),
                user.getCountry(),
                user.getBirthYear(),
                user.getBirthMonth(),
                user.getBirthDay(),
                user.getPhoneCountry(),
                user.getPhoneDigits(),
                user.getContactMethod()
        );
    }
}
