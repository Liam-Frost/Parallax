package com.parallax.backend.auth;

import com.parallax.backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResult result = authService.login(request, httpRequest);
        ResponseCookie cookie = authService.buildRefreshCookie(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(true, result.accessToken(), jwtService.getAccessTtlMinutes() * 60L, result.user()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResult result = authService.register(request, httpRequest);
        ResponseCookie cookie = authService.buildRefreshCookie(result.refreshToken());
        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(true, result.accessToken(), jwtService.getAccessTtlMinutes() * 60L, result.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(value = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshToken,
                                                HttpServletRequest httpRequest) {
        AuthResult result = authService.refresh(refreshToken, httpRequest);
        ResponseCookie cookie = authService.buildRefreshCookie(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(true, result.accessToken(), jwtService.getAccessTtlMinutes() * 60L, result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@CookieValue(value = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie cookie = authService.clearRefreshCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("success", true));
    }

    @PostMapping("/logout_all")
    public ResponseEntity<Map<String, Object>> logoutAll(
            @CookieValue(value = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logoutAll(refreshToken);
        ResponseCookie cookie = authService.clearRefreshCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("success", true));
    }
}
