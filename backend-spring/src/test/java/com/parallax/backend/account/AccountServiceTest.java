package com.parallax.backend.account;

import com.parallax.backend.auth.RefreshSessionRepository;
import com.parallax.backend.common.ApiException;
import com.parallax.backend.security.CurrentUserService;
import com.parallax.backend.user.UserEntity;
import com.parallax.backend.user.UserRepository;
import com.parallax.backend.user.UserRole;
import com.parallax.backend.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    CurrentUserService currentUserService;

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshSessionRepository refreshSessionRepository;

    AccountService accountService;

    @BeforeEach
    void setup() {
        accountService = new AccountService(currentUserService, userRepository, refreshSessionRepository, passwordEncoder);
    }

    @Test
    void updateContact_requiresPhone() {
        UserEntity user = baseUser();
        when(currentUserService.requireUser()).thenReturn(user);

        UpdateContactRequest request = new UpdateContactRequest("new@example.com", null, null, "Password123");

        assertThatThrownBy(() -> accountService.updateContact(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Phone is required");
    }

    @Test
    void updateContact_rejectsDuplicateEmail() {
        UserEntity user = baseUser();
        when(currentUserService.requireUser()).thenReturn(user);
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.of(otherUser()));

        UpdateContactRequest request = new UpdateContactRequest("new@example.com", "+1", "6041234567", "Password123");

        assertThatThrownBy(() -> accountService.updateContact(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updatePassword_revokesAllSessions() {
        UserEntity user = baseUser();
        when(currentUserService.requireUser()).thenReturn(user);
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepository).save(any(UserEntity.class));

        UpdatePasswordRequest request = new UpdatePasswordRequest("Password123", "NewPass123", "NewPass123", "ABCD");
        accountService.updatePassword(request);

        verify(refreshSessionRepository).deleteAllByUserId(user.getId());
    }

    @Test
    void deleteAccount_deniesAdmin() {
        UserEntity admin = baseUser();
        admin.setRole(UserRole.ADMIN);
        when(currentUserService.requireUser()).thenReturn(admin);

        assertThatThrownBy(() -> accountService.deleteAccount(new DeleteAccountRequest("Password123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Admin account");
    }

    private UserEntity baseUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setPhoneCountry("+1");
        user.setPhoneDigits("6041234567");
        return user;
    }

    private UserEntity otherUser() {
        UserEntity other = new UserEntity();
        other.setId(UUID.randomUUID());
        other.setEmail("other@example.com");
        other.setRole(UserRole.USER);
        other.setStatus(UserStatus.ACTIVE);
        other.setPasswordHash(passwordEncoder.encode("Password123"));
        other.setPhoneCountry("+1");
        other.setPhoneDigits("7781239876");
        return other;
    }
}
