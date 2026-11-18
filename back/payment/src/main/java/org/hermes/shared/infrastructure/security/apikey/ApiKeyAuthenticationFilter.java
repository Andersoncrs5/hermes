package org.hermes.shared.infrastructure.security.apikey;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.modules.apikey.domain.entity.ApiKeyPermissionEntity;
import org.hermes.modules.apikey.ports.out.ApiKeyPermissionRepository;
import org.hermes.modules.apikey.ports.out.ApiKeyRepository;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.security.context.TenantSecurityContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION - 10)
public class ApiKeyAuthenticationFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Inject
    TenantSecurityContext tenantSecurityContext;

    @Inject
    Instance<ApiKeyRepository> apiKeyRepositoryInstance;

    @Inject
    Instance<ApiKeyPermissionRepository> apiKeyPermissionRepositoryInstance;

    @Inject
    Instance<PermissionRepository> permissionRepositoryInstance;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String apiKeyHeader = requestContext.getHeaderString(API_KEY_HEADER);
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            return;
        }

        if (apiKeyRepositoryInstance.isUnsatisfied()) {
            return;
        }

        String rawKey = apiKeyHeader.trim();
        if (rawKey.length() < 16) {
            abortWithResponse(requestContext, Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid API key format");
            return;
        }

        String keyPrefix = rawKey.substring(0, 16);
        ApiKeyRepository keyRepo = apiKeyRepositoryInstance.get();
        Optional<ApiKeyEntity> keyOpt = keyRepo.findByPrefix(keyPrefix);

        if (keyOpt.isEmpty()) {
            abortWithResponse(requestContext, Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid API key");
            return;
        }

        ApiKeyEntity apiKey = keyOpt.get();

        if (!apiKey.isValid()) {
            if (apiKey.isExpired()) {
                abortWithResponse(requestContext, Response.Status.UNAUTHORIZED.getStatusCode(), "API key has expired");
            } else {
                abortWithResponse(requestContext, Response.Status.UNAUTHORIZED.getStatusCode(), "API key is revoked or inactive");
            }
            return;
        }

        String computedHash = hashSecret(rawKey);
        if (!computedHash.equals(apiKey.getSecretHash())) {
            abortWithResponse(requestContext, Response.Status.UNAUTHORIZED.getStatusCode(), "Invalid API key secret");
            return;
        }

        apiKey.recordUsage();
        keyRepo.save(apiKey);

        tenantSecurityContext.setTenantId(apiKey.getTenantId());
        tenantSecurityContext.setApiKey(true);
        tenantSecurityContext.setApiKeyId(apiKey.getId());
        tenantSecurityContext.setAuthenticated(true);

        if (!apiKeyPermissionRepositoryInstance.isUnsatisfied() && !permissionRepositoryInstance.isUnsatisfied()) {
            ApiKeyPermissionRepository akpRepo = apiKeyPermissionRepositoryInstance.get();
            PermissionRepository permRepo = permissionRepositoryInstance.get();

            List<ApiKeyPermissionEntity> akPerms = akpRepo.findByApiKeyId(apiKey.getId());
            Set<String> permissions = new HashSet<>();
            for (ApiKeyPermissionEntity akp : akPerms) {
                permRepo.findById(akp.getPermissionId()).ifPresent(p -> permissions.add(p.getName()));
            }
            tenantSecurityContext.setPermissions(permissions);
        }
    }

    private String hashSecret(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
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
