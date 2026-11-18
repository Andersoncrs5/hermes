package org.hermes.modules.authorization.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class RoleEntity extends BaseEntity {

    private String name;
    private String slug;
    private String description;
    private Boolean isActive = true;
    private Boolean isSystem = false;

    public RoleEntity() {
    }

    public RoleEntity(Long id, String name, String slug, String description, Boolean isActive, Boolean isSystem, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
        this.isSystem = isSystem != null ? isSystem : false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean system) {
        isSystem = system;
    }
}
