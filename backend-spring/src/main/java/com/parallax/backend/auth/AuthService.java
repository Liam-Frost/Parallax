package com.parallax.backend.auth;

import com.parallax.backend.security.JwtService;
import com.parallax.backend.user.UserEntity;
import com.parallax.backend.user.UserRepository;
import com.parallax.backend.user.UserRole;
import com.parallax.backend.user.UserStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    public static final String REFRESH_COOKIE_NAME = "parallax_refresh";

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshSessionRepository refreshSessionRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResult login(LoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || isBlank(request.identifier()) || isBlank(request.password())) {
            throw new AuthException("INVALID_CREDENTIALS", "Identifier and password are required");
        }

        String identifier = request.identifier().trim().toLowerCase(Locale.ROOT);
        Optional<UserEntity> user = userRepository.findByEmailIgnoreCase(identifier);
        if (user.isEmpty()) {
            user = resolveByPhone(identifier);
        }

        UserEntity found = user.orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "Invalid credentials"));
        if (found.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("ACCOUNT_DISABLED", "Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), found.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        return issueTokens(found, httpRequest);
    }

    public AuthResult register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw new AuthException("INVALID_REQUEST", "Email and password are required");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new AuthException("EMAIL_EXISTS", "Email already exists");
        }

        String phoneCountry = normalizeNullable(request.phoneCountry());
        String phoneDigits = normalizeDigitsNullable(request.phone());
        if (phoneCountry == null || phoneDigits == null) {
            throw new AuthException("INVALID_REQUEST", "Phone is required");
        }

        if (userRepository.findByPhoneCountryAndPhoneDigits(phoneCountry, phoneDigits).isPresent()) {
            throw new AuthException("PHONE_EXISTS", "Phone already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setDisplayName(buildDisplayName(request.firstName(), request.lastName(), email));
        user.setCountry(request.country());
        user.setBirthYear(request.birthYear());
        user.setBirthMonth(request.birthMonth());
        user.setBirthDay(request.birthDay());
        user.setPhoneCountry(phoneCountry);
        user.setPhoneDigits(phoneDigits);
        user.setContactMethod(request.contactMethod());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        UserEntity saved = userRepository.save(user);
        return issueTokens(saved, httpRequest);
    }

    public AuthResult refresh(String refreshToken, HttpServletRequest httpRequest) {
        if (isBlank(refreshToken)) {
            throw new AuthException("REFRESH_REQUIRED", "Refresh token is required");
        }

        byte[] hash = hashToken(refreshToken);
        RefreshSessionEntity session = refreshSessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new AuthException("REFRESH_INVALID", "Refresh token is invalid"));

        if (session.getRevokedAt() != null || session.getReplacedBySid() != null) {
            revokeSession(session, "REFRESH_REUSED");
            throw new AuthException("REFRESH_REUSED", "Refresh token was already used");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            revokeSession(session, "REFRESH_EXPIRED");
            throw new AuthException("REFRESH_EXPIRED", "Refresh token expired");
        }

        UserEntity user = session.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            revokeSession(session, "ACCOUNT_DISABLED");
            throw new AuthException("ACCOUNT_DISABLED", "Account is disabled");
        }

        RefreshSessionEntity newSession = createSession(user, httpRequest);
        session.setReplacedBySid(newSession.getSid());
        session.setLastUsedAt(Instant.now());
        refreshSessionRepository.save(session);

        String access = jwtService.createAccessToken(user, newSession.getSid());
        return new AuthResult(access, newSession.getRefreshTokenPlain(), buildUserView(user));
    }

    public void logout(String refreshToken) {
        revokeCurrentSession();

        if (!isBlank(refreshToken)) {
            byte[] hash = hashToken(refreshToken);
            refreshSessionRepository.findByRefreshTokenHash(hash)
                    .ifPresent(session -> revokeSession(session, "LOGOUT"));
        }
    }

    public void logoutAll(String refreshToken) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            UUID userId = UUID.fromString(auth.getPrincipal().toString());
            refreshSessionRepository.findAllByUserId(userId)
                    .forEach(session -> revokeSession(session, "LOGOUT_ALL"));
        }
        if (!isBlank(refreshToken)) {
            logout(refreshToken);
        }
    }

    public ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(jwtService.getRefreshTtlDays() * 86400L)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();
    }

    private AuthResult issueTokens(UserEntity user, HttpServletRequest request) {
        RefreshSessionEntity session = createSession(user, request);
        String access = jwtService.createAccessToken(user, session.getSid());
        return new AuthResult(access, session.getRefreshTokenPlain(), buildUserView(user));
    }

    private RefreshSessionEntity createSession(UserEntity user, HttpServletRequest request) {
        String refreshToken = TokenGenerator.generateToken();
        RefreshSessionEntity session = new RefreshSessionEntity();
        session.setUser(user);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setExpiresAt(Instant.now().plus(jwtService.getRefreshTtlDays(), ChronoUnit.DAYS));
        session.setIpLast(request.getRemoteAddr());
        session.setUaLast(request.getHeader("User-Agent"));
        RefreshSessionEntity saved = refreshSessionRepository.save(session);
        saved.setRefreshTokenPlain(refreshToken);
        return saved;
    }

    private void revokeSession(RefreshSessionEntity session, String reason) {
        session.setRevokedAt(Instant.now());
        session.setRevokedReason(reason);
        refreshSessionRepository.save(session);
    }

    private Optional<UserEntity> resolveByPhone(String identifier) {
        String digits = normalizeDigits(identifier);
        if (isBlank(digits)) {
            return Optional.empty();
        }
        return userRepository.findByPhoneDigits(digits);
    }

    private AuthUserView buildUserView(UserEntity user) {
        return new AuthUserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }

    private String buildDisplayName(String firstName, String lastName, String email) {
        String first = normalize(firstName);
        String last = normalize(lastName);
        String combined = (first + " " + last).trim();
        return combined.isBlank() ? email : combined;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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

    private String normalizeDigits(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private byte[] hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }

    private void revokeCurrentSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            return;
        }
        String token = auth.getCredentials().toString();
        try {
            Claims claims = jwtService.parseAccessToken(token);
            String sid = claims.get("sid", String.class);
            if (sid == null || sid.isBlank()) {
                return;
            }
            UUID sessionId = UUID.fromString(sid);
            refreshSessionRepository.findById(sessionId)
                    .ifPresent(session -> revokeSession(session, "LOGOUT"));
        } catch (Exception ignored) {
            // ignore
        }
    }
}
