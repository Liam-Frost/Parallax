package com.parallax.backend.account;

import com.parallax.backend.auth.RefreshSessionRepository;
import com.parallax.backend.common.ApiException;
import com.parallax.backend.security.CurrentUserService;
import com.parallax.backend.user.UserEntity;
import com.parallax.backend.user.UserRepository;
import com.parallax.backend.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Service
public class AccountService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(CurrentUserService currentUserService,
                          UserRepository userRepository,
                          RefreshSessionRepository refreshSessionRepository,
                          PasswordEncoder passwordEncoder) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void updateContact(UpdateContactRequest request) {
        UserEntity user = currentUserService.requireUser();
        if (request == null || isBlank(request.currentPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "Current password is required");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "Invalid password");
        }

        String email = normalizeEmail(request.email());
        String phoneCountry = normalizeNullable(request.phoneCountry());
        String phoneDigits = normalizeDigitsNullable(request.phone());

        if (isBlank(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "Email is required");
        }

        if (phoneCountry == null || phoneDigits == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHONE_REQUIRED", "Phone is required");
        }

        userRepository.findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(user.getId()))
                .ifPresent(found -> {
                    throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already exists");
                });

        userRepository.findByPhoneCountryAndPhoneDigits(phoneCountry, phoneDigits)
                .filter(found -> !found.getId().equals(user.getId()))
                .ifPresent(found -> {
                    throw new ApiException(HttpStatus.CONFLICT, "PHONE_EXISTS", "Phone already exists");
                });

        user.setEmail(email);
        user.setPhoneCountry(phoneCountry);
        user.setPhoneDigits(phoneDigits);
        userRepository.save(user);
    }

    public void updatePassword(UpdatePasswordRequest request) {
        UserEntity user = currentUserService.requireUser();
        if (request == null || isBlank(request.oldPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "Current password is required");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "Invalid password");
        }
        if (isBlank(request.newPassword()) || isBlank(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "New password is required");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Password mismatch");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshSessionRepository.deleteAllByUserId(user.getId());
    }

    public void deleteAccount(DeleteAccountRequest request) {
        UserEntity user = currentUserService.requireUser();
        if (user.getRole() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN_ACCOUNT_CANNOT_BE_DELETED", "Admin account cannot be deleted");
        }
        if (request == null || isBlank(request.currentPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "Current password is required");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "Invalid password");
        }
        refreshSessionRepository.deleteAllByUserId(user.getId());
        userRepository.delete(user);
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed;
    }

    private String normalizeDigitsNullable(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        return digits;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
