package com.parallax.backend.security;

import com.parallax.backend.user.UserEntity;
import com.parallax.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void createAndParseAccessToken_containsExpectedClaims() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        JwtProperties properties = new JwtProperties();
        properties.setIssuer("parallax");
        properties.setAudience("parallax-api");
        properties.setAccessTtlMinutes(15);
        properties.setRefreshTtlDays(30);
        properties.setPrivateKey(toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        properties.setPublicKey(toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));

        JwtService jwtService = new JwtService(properties);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.USER);

        UUID sid = UUID.randomUUID();
        String token = jwtService.createAccessToken(user, sid);

        var claims = jwtService.parseAccessToken(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("sid", String.class)).isEqualTo(sid.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
    }

    private String toPem(String type, byte[] der) {
        String base64 = Base64.getEncoder().encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }
}
