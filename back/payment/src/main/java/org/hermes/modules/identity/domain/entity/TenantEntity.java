package org.hermes.modules.identity.domain.entity;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.time.OffsetDateTime;

public class TenantEntity extends BaseEntity {

    private String name;
    private String slug;
    private String status = "ACTIVE";
    private String countryCode = "BR";

    public TenantEntity() {
    }

    public TenantEntity(Long id, String name, String slug, String status, String countryCode, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime deletedAt) {
        super(id, version, createdAt, updatedAt, deletedAt);
        this.name = name;
        this.slug = slug;
        this.status = status != null ? status : "ACTIVE";
        this.countryCode = countryCode != null ? countryCode : "BR";
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status) && !isDeleted();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
