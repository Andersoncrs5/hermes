package org.hermes.modules.identity.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class UserTenantEntity extends BaseEntity {

    private Long userId;
    private Long tenantId;
    private String status = "ACTIVE";
    private OffsetDateTime joinedAt;
    private Long invitedBy;

    public UserTenantEntity() {
    }

    public UserTenantEntity(Long id, Long userId, Long tenantId, String status, OffsetDateTime joinedAt, Long invitedBy, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.userId = userId;
        this.tenantId = tenantId;
        this.status = status != null ? status : "ACTIVE";
        this.joinedAt = joinedAt != null ? joinedAt : OffsetDateTime.now();
        this.invitedBy = invitedBy;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status) && !isDeleted();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }
}
