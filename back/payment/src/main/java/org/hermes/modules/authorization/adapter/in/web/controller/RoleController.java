package org.hermes.modules.authorization.adapter.in.web.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hermes.modules.authorization.application.dto.AssignRoleToUserTenantDTO;
import org.hermes.modules.authorization.application.dto.CreateRoleDTO;
import org.hermes.modules.authorization.application.dto.PermissionDTO;
import org.hermes.modules.authorization.application.dto.RoleDTO;
import org.hermes.modules.authorization.ports.in.IAssignPermissionToRoleUseCase;
import org.hermes.modules.authorization.ports.in.IAssignRoleToUserTenantUseCase;
import org.hermes.modules.authorization.ports.in.ICreateRoleUseCase;
import org.hermes.modules.authorization.ports.in.IListPermissionsUseCase;
import org.hermes.modules.authorization.ports.in.IListRolesUseCase;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.idempotency.annotation.Idempotent;
import org.hermes.shared.infrastructure.security.abac.PermissionsAllowed;
import org.hermes.shared.result.Result;

import java.util.List;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class RoleController {

    private final ICreateRoleUseCase createRoleUseCase;
    private final IListRolesUseCase listRolesUseCase;
    private final IListPermissionsUseCase listPermissionsUseCase;
    private final IAssignRoleToUserTenantUseCase assignRoleToUserTenantUseCase;
    private final IAssignPermissionToRoleUseCase assignPermissionToRoleUseCase;

    @Inject
    public RoleController(
            ICreateRoleUseCase createRoleUseCase,
            IListRolesUseCase listRolesUseCase,
            IListPermissionsUseCase listPermissionsUseCase,
            IAssignRoleToUserTenantUseCase assignRoleToUserTenantUseCase,
            IAssignPermissionToRoleUseCase assignPermissionToRoleUseCase
    ) {
        this.createRoleUseCase = createRoleUseCase;
        this.listRolesUseCase = listRolesUseCase;
        this.listPermissionsUseCase = listPermissionsUseCase;
        this.assignRoleToUserTenantUseCase = assignRoleToUserTenantUseCase;
        this.assignPermissionToRoleUseCase = assignPermissionToRoleUseCase;
    }

    @GET
    @Path("/roles")
    @PermissionsAllowed({"iam.role.read"})
    @RunOnVirtualThread
    public Response listRoles() {
        Result<List<RoleDTO>> result = listRolesUseCase.execute();

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "Roles listed successfully"))
                .build();
    }

    @POST
    @Path("/roles")
    @Idempotent
    @PermissionsAllowed({"iam.role.manage"})
    @RunOnVirtualThread
    public Response createRole(
            @Valid CreateRoleDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<RoleDTO> result = createRoleUseCase.execute(dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "Role created successfully"))
                .build();
    }

    @POST
    @Path("/roles/{roleId}/permissions/{permissionId}")
    @PermissionsAllowed({"iam.role.manage"})
    @RunOnVirtualThread
    public Response assignPermissionToRole(
            @PathParam("roleId") Long roleId,
            @PathParam("permissionId") Long permissionId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = assignPermissionToRoleUseCase.execute(roleId, permissionId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "Permission assigned to role successfully"))
                .build();
    }

    @POST
    @Path("/roles/assign-user-tenant")
    @PermissionsAllowed({"iam.role.manage"})
    @RunOnVirtualThread
    public Response assignRoleToUserTenant(
            @Valid AssignRoleToUserTenantDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = assignRoleToUserTenantUseCase.execute(dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "Role assigned to user tenant successfully"))
                .build();
    }

    @GET
    @Path("/permissions")
    @PermissionsAllowed({"iam.permission.read"})
    @RunOnVirtualThread
    public Response listPermissions() {
        Result<List<PermissionDTO>> result = listPermissionsUseCase.execute();

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "Permissions listed successfully"))
                .build();
    }
}
