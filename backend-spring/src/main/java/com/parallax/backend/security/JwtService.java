package com.parallax.backend.security;

import com.parallax.backend.user.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.privateKey = loadPrivateKey(properties.getPrivateKey());
        this.publicKey = loadPublicKey(properties.getPublicKey());
    }

    public String createAccessToken(UserEntity user, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTtlMinutes() * 60L);
        Map<String, Object> claims = Map.of(
                "sid", sessionId.toString(),
                "role", user.getRole().name()
        );

        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setAudience(properties.getAudience())
                .setSubject(user.getId().toString())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .addClaims(claims)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public int getAccessTtlMinutes() {
        return properties.getAccessTtlMinutes();
    }

    public int getRefreshTtlDays() {
        return properties.getRefreshTtlDays();
    }

    private PrivateKey loadPrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("JWT private key must be configured");
        }
        try {
            byte[] decoded = decodePem(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT private key", e);
        }
    }

    private PublicKey loadPublicKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("JWT public key must be configured");
        }
        try {
            byte[] decoded = decodePem(pem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key", e);
        }
    }

    private byte[] decodePem(String pem) {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }
}
