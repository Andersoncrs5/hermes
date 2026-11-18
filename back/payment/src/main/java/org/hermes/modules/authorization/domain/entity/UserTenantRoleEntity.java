package org.hermes.modules.authorization.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class UserTenantRoleEntity extends BaseEntity {

    private Long userTenantId;
    private Long roleId;
    private Boolean isActive = true;

    public UserTenantRoleEntity() {
    }

    public UserTenantRoleEntity(Long id, Long userTenantId, Long roleId, Boolean isActive, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.userTenantId = userTenantId;
        this.roleId = roleId;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getUserTenantId() {
        return userTenantId;
    }

    public void setUserTenantId(Long userTenantId) {
        this.userTenantId = userTenantId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
