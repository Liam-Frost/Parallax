package com.parallax.backend.auth;

import com.parallax.backend.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSessionEntity {
    @Id
    private UUID sid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "refresh_token_hash", nullable = false)
    private byte[] refreshTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "replaced_by_sid")
    private UUID replacedBySid;

    @Column(name = "ip_last")
    private String ipLast;

    @Column(name = "ua_last")
    private String uaLast;

    @Transient
    private String refreshTokenPlain;

    @PrePersist
    void onCreate() {
        if (sid == null) {
            sid = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        if (lastUsedAt == null) {
            lastUsedAt = Instant.now();
        }
    }

    public UUID getSid() {
        return sid;
    }

    public void setSid(UUID sid) {
        this.sid = sid;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public byte[] getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(byte[] refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public UUID getReplacedBySid() {
        return replacedBySid;
    }

    public void setReplacedBySid(UUID replacedBySid) {
        this.replacedBySid = replacedBySid;
    }

    public String getIpLast() {
        return ipLast;
    }

    public void setIpLast(String ipLast) {
        this.ipLast = ipLast;
    }

    public String getUaLast() {
        return uaLast;
    }

    public void setUaLast(String uaLast) {
        this.uaLast = uaLast;
    }

    public String getRefreshTokenPlain() {
        return refreshTokenPlain;
    }

    public void setRefreshTokenPlain(String refreshTokenPlain) {
        this.refreshTokenPlain = refreshTokenPlain;
    }
}
