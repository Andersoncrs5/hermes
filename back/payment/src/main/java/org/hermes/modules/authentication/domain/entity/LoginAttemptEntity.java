package org.hermes.modules.authentication.domain.entity;

import java.time.OffsetDateTime;

public class LoginAttemptEntity {

    private Long id;
    private Long userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String deviceId;
    private OffsetDateTime attemptAt;
    private Boolean success;
    private String failureReason;
    private String traceId;

    public LoginAttemptEntity() {
    }

    public LoginAttemptEntity(Long id, Long userId, String username, String ipAddress, String userAgent, String deviceId, OffsetDateTime attemptAt, Boolean success, String failureReason, String traceId) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceId = deviceId;
        this.attemptAt = attemptAt != null ? attemptAt : OffsetDateTime.now();
        this.success = success != null ? success : false;
        this.failureReason = failureReason;
        this.traceId = traceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public OffsetDateTime getAttemptAt() {
        return attemptAt;
    }

    public void setAttemptAt(OffsetDateTime attemptAt) {
        this.attemptAt = attemptAt;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
