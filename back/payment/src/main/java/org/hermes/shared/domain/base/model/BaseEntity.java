package org.hermes.shared.domain.base.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public abstract class BaseEntity {

    protected Long id;
    protected Long version;
    protected OffsetDateTime createdAt;
    protected OffsetDateTime updatedAt;
    protected OffsetDateTime deletedAt;

    public BaseEntity() {
    }

    public BaseEntity(Long id, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        this.id = id;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void incrementVersion() {
        this.version = (this.version == null) ? 1L : this.version + 1L;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
