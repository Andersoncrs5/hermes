package org.hermes.modules.authentication.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class SessionEntity extends BaseEntity {

    private Long userId;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime lastActivityAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime revokedAt;
    private String revocationReason;
    private Boolean isActive = true;

    public SessionEntity() {
    }

    public SessionEntity(Long id, Long userId, String deviceId, String ipAddress, String userAgent, OffsetDateTime lastActivityAt, OffsetDateTime expiresAt, OffsetDateTime revokedAt, String revocationReason, Boolean isActive, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.userId = userId;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastActivityAt = lastActivityAt != null ? lastActivityAt : OffsetDateTime.now();
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.revocationReason = revocationReason;
        this.isActive = isActive != null ? isActive : true;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && revokedAt == null && !isExpired() && !isDeleted();
    }

    public void revoke(String reason) {
        this.isActive = false;
        this.revokedAt = OffsetDateTime.now();
        this.revocationReason = reason;
        touch();
    }

    public void refreshActivity(int sessionInactivitySeconds) {
        this.lastActivityAt = OffsetDateTime.now();
        this.expiresAt = this.lastActivityAt.plusSeconds(sessionInactivitySeconds);
        touch();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
