package org.hermes.modules.identity.adapter.in.web.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.modules.identity.application.dto.UpdateUserDTO;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.modules.identity.ports.in.IFindUserByIdUseCase;
import org.hermes.modules.identity.ports.in.IUpdateUserProfileUseCase;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.security.abac.PermissionsAllowed;
import org.hermes.shared.result.Result;

@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserController {

    private final IFindUserByIdUseCase findUserByIdUseCase;
    private final IUpdateUserProfileUseCase updateUserProfileUseCase;
    private final JsonWebToken jwt;

    @Inject
    public UserController(
            IFindUserByIdUseCase findUserByIdUseCase,
            IUpdateUserProfileUseCase updateUserProfileUseCase,
            JsonWebToken jwt
    ) {
        this.findUserByIdUseCase = findUserByIdUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.jwt = jwt;
    }

    @GET
    @Path("/me")
    @RunOnVirtualThread
    public Response me() {
        Long userId = Long.valueOf(jwt.getSubject());
        Result<UserDTO> result = findUserByIdUseCase.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "Profile retrieved successfully"))
                .build();
    }

    @PUT
    @Path("/me")
    @RunOnVirtualThread
    public Response updateMe(@Valid UpdateUserDTO dto) {
        Long userId = Long.valueOf(jwt.getSubject());
        Result<UserDTO> result = updateUserProfileUseCase.execute(userId, dto);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "Profile updated successfully"))
                .build();
    }

    @GET
    @Path("/{id}")
    @PermissionsAllowed({"iam.user.read"})
    @RunOnVirtualThread
    public Response getById(@PathParam("id") Long userId) {
        Result<UserDTO> result = findUserByIdUseCase.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), null, "User retrieved successfully"))
                .build();
    }
}
