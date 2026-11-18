package org.hermes.modules.authorization.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class UserPermissionEntity extends BaseEntity {

    private Long userTenantId;
    private Long permissionId;
    private Boolean isActive = true;

    public UserPermissionEntity() {
    }

    public UserPermissionEntity(Long id, Long userTenantId, Long permissionId, Boolean isActive, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.userTenantId = userTenantId;
        this.permissionId = permissionId;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getUserTenantId() {
        return userTenantId;
    }

    public void setUserTenantId(Long userTenantId) {
        this.userTenantId = userTenantId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
