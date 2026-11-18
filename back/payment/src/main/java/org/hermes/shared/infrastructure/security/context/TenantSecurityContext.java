package org.hermes.shared.infrastructure.security.context;

import jakarta.enterprise.context.RequestScoped;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RequestScoped
public class TenantSecurityContext {

    private Long userId;
    private Long tenantId;
    private Set<String> roles = new HashSet<>();
    private Set<String> permissions = new HashSet<>();
    private boolean authenticated = false;
    private boolean isApiKey = false;
    private Long apiKeyId;

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

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }

    public void addRole(String role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }

    public void addPermission(String permission) {
        if (permission != null) {
            this.permissions.add(permission);
        }
    }

    public boolean hasPermission(String permission) {
        return this.permissions.contains(permission);
    }

    public boolean hasAnyPermission(String... requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        for (String perm : requiredPermissions) {
            if (this.permissions.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isApiKey() {
        return isApiKey;
    }

    public void setApiKey(boolean apiKey) {
        isApiKey = apiKey;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
}
