package org.hermes.modules.authentication.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class RefreshTokenEntity extends BaseEntity {

    private Long sessionId;
    private Long userId;
    private String tokenHash;
    private Boolean isUsed = false;
    private Boolean isActive = true;
    private Long replacedByTokenId;
    private OffsetDateTime expiredAt;
    private OffsetDateTime revokedAt;

    public RefreshTokenEntity() {
    }

    public RefreshTokenEntity(Long id, Long sessionId, Long userId, String tokenHash, Boolean isUsed, Boolean isActive, Long replacedByTokenId, OffsetDateTime expiredAt, OffsetDateTime revokedAt, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.sessionId = sessionId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.isUsed = isUsed != null ? isUsed : false;
        this.isActive = isActive != null ? isActive : true;
        this.replacedByTokenId = replacedByTokenId;
        this.expiredAt = expiredAt;
        this.revokedAt = revokedAt;
    }

    public boolean isExpired() {
        return expiredAt != null && expiredAt.isBefore(OffsetDateTime.now());
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !Boolean.TRUE.equals(isUsed) && revokedAt == null && !isExpired() && !isDeleted();
    }

    public void markAsUsed(Long newTokenId) {
        this.isUsed = true;
        this.replacedByTokenId = newTokenId;
        touch();
    }

    public void revoke() {
        this.isActive = false;
        this.revokedAt = OffsetDateTime.now();
        touch();
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean used) {
        isUsed = used;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Long getReplacedByTokenId() {
        return replacedByTokenId;
    }

    public void setReplacedByTokenId(Long replacedByTokenId) {
        this.replacedByTokenId = replacedByTokenId;
    }

    public OffsetDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(OffsetDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
