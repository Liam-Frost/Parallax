package com.parallax.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {
    Optional<RefreshSessionEntity> findByRefreshTokenHash(byte[] refreshTokenHash);
    List<RefreshSessionEntity> findAllByUserId(UUID userId);
    List<RefreshSessionEntity> findAllByExpiresAtBefore(Instant cutoff);
    void deleteAllByUserId(UUID userId);
}
