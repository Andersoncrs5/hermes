package org.hermes.modules.audit.domain.entity;

import java.time.OffsetDateTime;

public class AuditLogEntity {

    private Long id;
    private Long tenantId;
    private String actorType;
    private Long actorId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String before;
    private String after;
    private String ipAddress;
    private String userAgent;
    private String traceId;
    private OffsetDateTime createdAt;

    public AuditLogEntity() {
    }

    public AuditLogEntity(Long id, Long tenantId, String actorType, Long actorId, String action, String resourceType, String resourceId, String before, String after, String ipAddress, String userAgent, String traceId, OffsetDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.before = before;
        this.after = after;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.traceId = traceId;
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
