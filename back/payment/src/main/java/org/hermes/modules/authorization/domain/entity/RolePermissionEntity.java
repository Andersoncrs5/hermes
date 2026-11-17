package org.hermes.modules.authorization.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class RolePermissionEntity extends BaseEntity {

    private Long roleId;
    private Long permissionId;

    public RolePermissionEntity() {
    }

    public RolePermissionEntity(Long id, Long roleId, Long permissionId, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
