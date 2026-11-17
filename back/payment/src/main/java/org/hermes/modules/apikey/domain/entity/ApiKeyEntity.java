package org.hermes.modules.apikey.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class ApiKeyEntity extends BaseEntity {

    private Long tenantId;
    private String keyPrefix;
    private String secretHash;
    private String name;
    private String status = "ACTIVE";
    private String environment = "TEST";
    private OffsetDateTime expiredAt;
    private Long createdBy;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime revokedAt;

    public ApiKeyEntity() {
    }

    public ApiKeyEntity(Long id, Long tenantId, String keyPrefix, String secretHash, String name, String status, String environment, OffsetDateTime expiredAt, Long createdBy, OffsetDateTime lastUsedAt, OffsetDateTime revokedAt, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.tenantId = tenantId;
        this.keyPrefix = keyPrefix;
        this.secretHash = secretHash;
        this.name = name;
        this.status = status != null ? status : "ACTIVE";
        this.environment = environment != null ? environment : "TEST";
        this.expiredAt = expiredAt;
        this.createdBy = createdBy;
        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
    }

    public boolean isExpired() {
        return expiredAt != null && expiredAt.isBefore(OffsetDateTime.now());
    }

    public boolean isValid() {
        return "ACTIVE".equalsIgnoreCase(this.status) && revokedAt == null && !isExpired() && !isDeleted();
    }

    public void revoke() {
        this.status = "REVOKED";
        this.revokedAt = OffsetDateTime.now();
        touch();
    }

    public void recordUsage() {
        this.lastUsedAt = OffsetDateTime.now();
        touch();
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public void setSecretHash(String secretHash) {
        this.secretHash = secretHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public OffsetDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(OffsetDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
