package org.hermes.modules.apikey.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class ApiKeyPermissionEntity extends BaseEntity {

    private Long apiKeyId;
    private Long permissionId;

    public ApiKeyPermissionEntity() {
    }

    public ApiKeyPermissionEntity(Long id, Long apiKeyId, Long permissionId, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.apiKeyId = apiKeyId;
        this.permissionId = permissionId;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
