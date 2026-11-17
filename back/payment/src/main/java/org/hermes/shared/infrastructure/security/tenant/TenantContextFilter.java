package org.hermes.shared.infrastructure.security.tenant;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.RolePermissionEntity;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.modules.authorization.ports.out.RolePermissionRepository;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.security.context.TenantSecurityContext;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class TenantContextFilter implements ContainerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Context
    ResourceInfo resourceInfo;

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantSecurityContext tenantSecurityContext;

    @Inject
    Instance<UserTenantRepository> userTenantRepositoryInstance;

    @Inject
    Instance<UserTenantRoleRepository> userTenantRoleRepositoryInstance;

    @Inject
    Instance<RoleRepository> roleRepositoryInstance;

    @Inject
    Instance<RolePermissionRepository> rolePermissionRepositoryInstance;

    @Inject
    Instance<PermissionRepository> permissionRepositoryInstance;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        boolean isTenantRequired = isTenantRequiredEndpoint();
        String tenantHeaderValue = requestContext.getHeaderString(TENANT_HEADER);

        if (tenantHeaderValue == null || tenantHeaderValue.isBlank()) {
            if (isTenantRequired) {
                abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(),
                        "Header '" + TENANT_HEADER + "' is required for this endpoint.");
            }
            return;
        }

        Long tenantId;
        try {
            tenantId = Long.valueOf(tenantHeaderValue.trim());
        } catch (NumberFormatException e) {
            abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(),
                    "Header '" + TENANT_HEADER + "' must be a valid Snowflake BIGINT ID.");
            return;
        }

        tenantSecurityContext.setTenantId(tenantId);

        String sub = jwt != null ? jwt.getSubject() : null;
        if (sub != null && !sub.isBlank()) {
            try {
                Long userId = Long.valueOf(sub);
                tenantSecurityContext.setUserId(userId);
                tenantSecurityContext.setAuthenticated(true);

                if (!userTenantRepositoryInstance.isUnsatisfied()) {
                    UserTenantRepository utRepo = userTenantRepositoryInstance.get();
                    Optional<UserTenantEntity> utOpt = utRepo.findByUserAndTenant(userId, tenantId);

                    if (utOpt.isEmpty() || !utOpt.get().isActive()) {
                        abortWithResponse(requestContext, Response.Status.FORBIDDEN.getStatusCode(),
                                "User does not have an active membership in tenant " + tenantId);
                        return;
                    }

                    UserTenantEntity userTenant = utOpt.get();
                    loadTenantRolesAndPermissions(userTenant.getId());
                }
            } catch (NumberFormatException e) {
                abortWithResponse(requestContext, Response.Status.UNAUTHORIZED.getStatusCode(),
                        "Invalid subject format in authentication token.");
            }
        }
    }

    private void loadTenantRolesAndPermissions(Long userTenantId) {
        if (userTenantRoleRepositoryInstance.isUnsatisfied() || roleRepositoryInstance.isUnsatisfied()) {
            return;
        }

        UserTenantRoleRepository utrRepo = userTenantRoleRepositoryInstance.get();
        RoleRepository roleRepo = roleRepositoryInstance.get();
        RolePermissionRepository rpRepo = rolePermissionRepositoryInstance.get();
        PermissionRepository permRepo = permissionRepositoryInstance.get();

        List<UserTenantRoleEntity> utRoles = utrRepo.findByUserTenantId(userTenantId);
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();

        for (UserTenantRoleEntity utRole : utRoles) {
            roleRepo.findById(utRole.getRoleId()).ifPresent(role -> {
                roles.add(role.getName());
                List<RolePermissionEntity> rolePerms = rpRepo.findByRoleId(role.getId());
                for (RolePermissionEntity rp : rolePerms) {
                    permRepo.findById(rp.getPermissionId()).ifPresent(p -> permissions.add(p.getName()));
                }
            });
        }

        tenantSecurityContext.setRoles(roles);
        tenantSecurityContext.setPermissions(permissions);
    }

    private boolean isTenantRequiredEndpoint() {
        if (resourceInfo == null || resourceInfo.getResourceMethod() == null) {
            return false;
        }
        return resourceInfo.getResourceMethod().isAnnotationPresent(TenantRequired.class)
                || resourceInfo.getResourceClass().isAnnotationPresent(TenantRequired.class);
    }

    private void abortWithResponse(ContainerRequestContext requestContext, int statusCode, String message) {
        requestContext.abortWith(
                Response.status(statusCode)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(ResponseHTTP.error(message))
                        .build()
        );
    }
}
