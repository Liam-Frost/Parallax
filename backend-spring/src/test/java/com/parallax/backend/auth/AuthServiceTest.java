package com.parallax.backend.auth;

import com.parallax.backend.security.JwtService;
import com.parallax.backend.user.UserEntity;
import com.parallax.backend.user.UserRepository;
import com.parallax.backend.user.UserRole;
import com.parallax.backend.user.UserStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshSessionRepository refreshSessionRepository;

    @Mock
    JwtService jwtService;

    @Mock
    HttpServletRequest httpServletRequest;

    AuthService authService;

    @Captor
    ArgumentCaptor<RefreshSessionEntity> sessionCaptor;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setup() {
        authService = new AuthService(userRepository, refreshSessionRepository, jwtService, passwordEncoder);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
    }

    @Test
    void register_createsUserAndIssuesTokens() {
        when(jwtService.getRefreshTtlDays()).thenReturn(30);
        when(jwtService.createAccessToken(any(UserEntity.class), any(UUID.class))).thenReturn("access");

        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneCountryAndPhoneDigits("+1", "6041234567")).thenReturn(Optional.empty());

        doAnswer(invocation -> invocation.getArgument(0)).when(userRepository).save(any(UserEntity.class));
        doAnswer(invocation -> {
            RefreshSessionEntity entity = invocation.getArgument(0);
            if (entity.getSid() == null) {
                entity.setSid(UUID.randomUUID());
            }
            return entity;
        }).when(refreshSessionRepository).save(any(RefreshSessionEntity.class));

        RegisterRequest request = new RegisterRequest(
                "new@example.com",
                "Password123",
                "Test",
                "User",
                "CA",
                2000,
                1,
                1,
                "+1",
                "6041234567",
                "text"
        );

        AuthResult result = authService.register(request, httpServletRequest);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.user().email()).isEqualTo("new@example.com");
        assertThat(result.user().role()).isEqualTo("USER");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_requiresPhone() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com",
                "Password123",
                "Test",
                "User",
                "CA",
                2000,
                1,
                1,
                null,
                null,
                "text"
        );

        assertThatThrownBy(() -> authService.register(request, httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Phone is required");
    }

    @Test
    void login_validatesPasswordAndIssuesTokens() {
        when(jwtService.getRefreshTtlDays()).thenReturn(30);
        when(jwtService.createAccessToken(any(UserEntity.class), any(UUID.class))).thenReturn("access");

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode("Password123"));

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            RefreshSessionEntity entity = invocation.getArgument(0);
            if (entity.getSid() == null) {
                entity.setSid(UUID.randomUUID());
            }
            return entity;
        }).when(refreshSessionRepository).save(any(RefreshSessionEntity.class));

        AuthResult result = authService.login(new LoginRequest("user@example.com", "Password123"), httpServletRequest);
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.user().id()).isEqualTo(user.getId());
        verify(userRepository, never()).findByPhoneDigits(anyString());
    }

    @Test
    void refresh_rotatesTokenAndMarksOldSessionReplaced() {
        when(jwtService.getRefreshTtlDays()).thenReturn(30);
        when(jwtService.createAccessToken(any(UserEntity.class), any(UUID.class))).thenReturn("access");

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hash");

        RefreshSessionEntity oldSession = new RefreshSessionEntity();
        oldSession.setSid(UUID.randomUUID());
        oldSession.setUser(user);
        oldSession.setRefreshTokenHash(new byte[] {1, 2, 3});
        oldSession.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(refreshSessionRepository.findByRefreshTokenHash(any(byte[].class))).thenReturn(Optional.of(oldSession));
        doAnswer(invocation -> {
            RefreshSessionEntity entity = invocation.getArgument(0);
            if (entity.getSid() == null) {
                entity.setSid(UUID.randomUUID());
            }
            return entity;
        }).when(refreshSessionRepository).save(any(RefreshSessionEntity.class));

        AuthResult result = authService.refresh("refresh-token", httpServletRequest);
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(oldSession.getReplacedBySid()).isNotNull();
        verify(refreshSessionRepository).save(oldSession);
    }

    @Test
    void refresh_reuseRevokesAndFails() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("hash");

        RefreshSessionEntity session = new RefreshSessionEntity();
        session.setSid(UUID.randomUUID());
        session.setUser(user);
        session.setRefreshTokenHash(new byte[] {1});
        session.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        session.setReplacedBySid(UUID.randomUUID());

        when(refreshSessionRepository.findByRefreshTokenHash(any(byte[].class))).thenReturn(Optional.of(session));
        doAnswer(invocation -> invocation.getArgument(0)).when(refreshSessionRepository).save(any(RefreshSessionEntity.class));

        assertThatThrownBy(() -> authService.refresh("refresh-token", httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("already used");

        assertThat(session.getRevokedAt()).isNotNull();
        assertThat(session.getRevokedReason()).isEqualTo("REFRESH_REUSED");
    }

    @Test
    void logout_revokesCurrentSessionBySidClaim() {
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();

        Claims claims = mock(Claims.class);
        when(claims.get("sid", String.class)).thenReturn(sid.toString());
        when(jwtService.parseAccessToken("access")).thenReturn(claims);

        RefreshSessionEntity session = new RefreshSessionEntity();
        session.setSid(sid);
        session.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshSessionRepository.findById(sid)).thenReturn(Optional.of(session));
        doAnswer(invocation -> invocation.getArgument(0)).when(refreshSessionRepository).save(any(RefreshSessionEntity.class));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), "access", List.of())
        );

        authService.logout(null);
        assertThat(session.getRevokedReason()).isEqualTo("LOGOUT");
        assertThat(session.getRevokedAt()).isNotNull();
    }
}
