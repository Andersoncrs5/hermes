package org.hermes.modules.identity.adapter.in.web.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.modules.identity.application.dto.CreateTenantDTO;
import org.hermes.modules.identity.application.dto.InviteUserToTenantDTO;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.modules.identity.application.dto.UserTenantDTO;
import org.hermes.modules.identity.ports.in.ICreateTenantUseCase;
import org.hermes.modules.identity.ports.in.IFindTenantByIdUseCase;
import org.hermes.modules.identity.ports.in.IInviteUserToTenantUseCase;
import org.hermes.modules.identity.ports.in.IListTenantsUseCase;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.idempotency.annotation.Idempotent;
import org.hermes.shared.infrastructure.security.abac.PermissionsAllowed;
import org.hermes.shared.result.Result;

import java.util.List;

@Path("/v1/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TenantController {

    private final ICreateTenantUseCase createTenantUseCase;
    private final IFindTenantByIdUseCase findTenantByIdUseCase;
    private final IListTenantsUseCase listTenantsUseCase;
    private final IInviteUserToTenantUseCase inviteUserToTenantUseCase;
    private final JsonWebToken jwt;

    @Inject
    public TenantController(
            ICreateTenantUseCase createTenantUseCase,
            IFindTenantByIdUseCase findTenantByIdUseCase,
            IListTenantsUseCase listTenantsUseCase,
            IInviteUserToTenantUseCase inviteUserToTenantUseCase,
            JsonWebToken jwt
    ) {
        this.createTenantUseCase = createTenantUseCase;
        this.findTenantByIdUseCase = findTenantByIdUseCase;
        this.listTenantsUseCase = listTenantsUseCase;
        this.inviteUserToTenantUseCase = inviteUserToTenantUseCase;
        this.jwt = jwt;
    }

    @POST
    @Idempotent
    @RunOnVirtualThread
    public Response create(
            @Valid CreateTenantDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        Result<TenantDTO> result = createTenantUseCase.execute(dto, userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "Tenant created successfully"))
                .build();
    }

    @GET
    @RunOnVirtualThread
    public Response listMyTenants() {
        Long userId = Long.valueOf(jwt.getSubject());
        Result<List<TenantDTO>> result = listTenantsUseCase.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "Tenants listed successfully"))
                .build();
    }

    @GET
    @Path("/{id}")
    @RunOnVirtualThread
    public Response getById(@PathParam("id") Long tenantId) {
        Result<TenantDTO> result = findTenantByIdUseCase.execute(tenantId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "Tenant retrieved successfully"))
                .build();
    }

    @POST
    @Path("/{id}/invitations")
    @PermissionsAllowed({"iam.tenant.manage", "iam.tenant.invite"})
    @RunOnVirtualThread
    public Response inviteUser(
            @PathParam("id") Long tenantId,
            @Valid InviteUserToTenantDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Long inviterUserId = Long.valueOf(jwt.getSubject());
        Result<UserTenantDTO> result = inviteUserToTenantUseCase.execute(tenantId, dto, inviterUserId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "User invited to tenant successfully"))
                .build();
    }
}
