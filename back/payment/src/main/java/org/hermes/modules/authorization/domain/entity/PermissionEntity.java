package org.hermes.modules.authorization.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class PermissionEntity extends BaseEntity {

    private String name;
    private String slug;
    private String module;
    private String action;
    private String description;
    private String status = "ACTIVE";
    private Boolean isSystem = false;

    public PermissionEntity() {
    }

    public PermissionEntity(Long id, String name, String slug, String module, String action, String description, String status, Boolean isSystem, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.name = name;
        this.slug = slug;
        this.module = module;
        this.action = action;
        this.description = description;
        this.status = status != null ? status : "ACTIVE";
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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean system) {
        isSystem = system;
    }
}
