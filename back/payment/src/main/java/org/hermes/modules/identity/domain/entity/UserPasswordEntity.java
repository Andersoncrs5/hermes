package org.hermes.modules.identity.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class UserPasswordEntity extends BaseEntity {

    private Long userId;
    private String passwordHash;
    private String typeHash = "argon2id";
    private Boolean shouldUpdate = false;

    public UserPasswordEntity() {
    }

    public UserPasswordEntity(Long id, Long userId, String passwordHash, String typeHash, Boolean shouldUpdate, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.typeHash = typeHash != null ? typeHash : "argon2id";
        this.shouldUpdate = shouldUpdate != null ? shouldUpdate : false;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTypeHash() {
        return typeHash;
    }

    public void setTypeHash(String typeHash) {
        this.typeHash = typeHash;
    }

    public Boolean getShouldUpdate() {
        return shouldUpdate;
    }

    public void setShouldUpdate(Boolean shouldUpdate) {
        this.shouldUpdate = shouldUpdate;
    }
}
