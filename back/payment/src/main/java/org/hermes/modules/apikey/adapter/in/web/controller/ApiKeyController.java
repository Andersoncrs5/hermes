package org.hermes.modules.apikey.adapter.in.web.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.modules.apikey.application.dto.ApiKeyDTO;
import org.hermes.modules.apikey.application.dto.ApiKeyResponseDTO;
import org.hermes.modules.apikey.application.dto.CreateApiKeyDTO;
import org.hermes.modules.apikey.ports.in.ICreateApiKeyUseCase;
import org.hermes.modules.apikey.ports.in.IListApiKeysUseCase;
import org.hermes.modules.apikey.ports.in.IRevokeApiKeyUseCase;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.idempotency.annotation.Idempotent;
import org.hermes.shared.infrastructure.security.abac.PermissionsAllowed;
import org.hermes.shared.infrastructure.security.context.TenantSecurityContext;
import org.hermes.shared.infrastructure.security.tenant.TenantRequired;
import org.hermes.shared.result.Result;

import java.util.List;

@Path("/v1/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@TenantRequired
public class ApiKeyController {

    private final ICreateApiKeyUseCase createApiKeyUseCase;
    private final IListApiKeysUseCase listApiKeysUseCase;
    private final IRevokeApiKeyUseCase revokeApiKeyUseCase;
    private final TenantSecurityContext tenantSecurityContext;
    private final JsonWebToken jwt;

    @Inject
    public ApiKeyController(
            ICreateApiKeyUseCase createApiKeyUseCase,
            IListApiKeysUseCase listApiKeysUseCase,
            IRevokeApiKeyUseCase revokeApiKeyUseCase,
            TenantSecurityContext tenantSecurityContext,
            JsonWebToken jwt
    ) {
        this.createApiKeyUseCase = createApiKeyUseCase;
        this.listApiKeysUseCase = listApiKeysUseCase;
        this.revokeApiKeyUseCase = revokeApiKeyUseCase;
        this.tenantSecurityContext = tenantSecurityContext;
        this.jwt = jwt;
    }

    @POST
    @Idempotent
    @PermissionsAllowed({"iam.apikey.create", "iam.apikey.manage"})
    @RunOnVirtualThread
    public Response createApiKey(
            @Valid CreateApiKeyDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Long tenantId = tenantSecurityContext.getTenantId();
        Long userId = Long.valueOf(jwt.getSubject());

        Result<ApiKeyResponseDTO> result = createApiKeyUseCase.execute(tenantId, userId, dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "API key created successfully"))
                .build();
    }

    @GET
    @PermissionsAllowed({"iam.apikey.read", "iam.apikey.manage"})
    @RunOnVirtualThread
    public Response listApiKeys() {
        Long tenantId = tenantSecurityContext.getTenantId();
        Result<List<ApiKeyDTO>> result = listApiKeysUseCase.execute(tenantId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "API keys listed successfully"))
                .build();
    }

    @DELETE
    @Path("/{id}")
    @PermissionsAllowed({"iam.apikey.revoke", "iam.apikey.manage"})
    @RunOnVirtualThread
    public Response revokeApiKey(
            @PathParam("id") Long apiKeyId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Long tenantId = tenantSecurityContext.getTenantId();
        Result<Void> result = revokeApiKeyUseCase.execute(tenantId, apiKeyId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "API key revoked successfully"))
                .build();
    }
}
