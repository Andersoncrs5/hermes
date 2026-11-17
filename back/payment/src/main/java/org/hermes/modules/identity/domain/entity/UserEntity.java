package org.hermes.modules.identity.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class UserEntity extends BaseEntity {

    private String email;
    private String username;
    private String fullName;
    private Boolean isActive = true;
    private Boolean isEmailVerified = false;
    private OffsetDateTime blockedAt;
    private Integer failedLoginAttempts = 0;
    private OffsetDateTime lastLoginAt;

    public UserEntity() {
    }

    public UserEntity(Long id, String email, String username, String fullName, Boolean isActive, Boolean isEmailVerified, OffsetDateTime blockedAt, Integer failedLoginAttempts, OffsetDateTime lastLoginAt, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.isActive = isActive != null ? isActive : true;
        this.isEmailVerified = isEmailVerified != null ? isEmailVerified : false;
        this.blockedAt = blockedAt;
        this.failedLoginAttempts = failedLoginAttempts != null ? failedLoginAttempts : 0;
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isBlocked() {
        return blockedAt != null && blockedAt.isAfter(OffsetDateTime.now());
    }

    public void blockUntil(OffsetDateTime until) {
        this.blockedAt = until;
    }

    public void unblock() {
        this.blockedAt = null;
        this.failedLoginAttempts = 0;
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(Boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public OffsetDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(OffsetDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
