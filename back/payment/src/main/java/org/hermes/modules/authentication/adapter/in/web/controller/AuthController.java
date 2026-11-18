package org.hermes.modules.authentication.adapter.in.web.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.modules.authentication.application.dto.LoginUserDTO;
import org.hermes.modules.authentication.application.dto.RefreshTokenRequestDTO;
import org.hermes.modules.authentication.ports.in.ILoginUserUseCase;
import org.hermes.modules.authentication.ports.in.ILogoutUseCase;
import org.hermes.modules.authentication.ports.in.IRefreshTokenUseCase;
import org.hermes.modules.authentication.ports.in.IRegisterUserUseCase;
import org.hermes.modules.authentication.ports.in.IRevokeAllUserAccessUseCase;
import org.hermes.modules.identity.application.dto.CreateUserDTO;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.infrastructure.idempotency.annotation.Idempotent;
import org.hermes.shared.result.Result;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private final IRegisterUserUseCase registerUserUseCase;
    private final ILoginUserUseCase loginUserUseCase;
    private final IRefreshTokenUseCase refreshTokenUseCase;
    private final ILogoutUseCase logoutUseCase;
    private final IRevokeAllUserAccessUseCase revokeAllUserAccessUseCase;
    private final JsonWebToken jwt;

    @Inject
    public AuthController(
            IRegisterUserUseCase registerUserUseCase,
            ILoginUserUseCase loginUserUseCase,
            IRefreshTokenUseCase refreshTokenUseCase,
            ILogoutUseCase logoutUseCase,
            IRevokeAllUserAccessUseCase revokeAllUserAccessUseCase,
            JsonWebToken jwt
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.revokeAllUserAccessUseCase = revokeAllUserAccessUseCase;
        this.jwt = jwt;
    }

    @POST
    @Path("/register")
    @Idempotent
    @RunOnVirtualThread
    public Response register(
            @Valid CreateUserDTO dto,
            @HeaderParam("X-Forwarded-For") String ipAddress,
            @HeaderParam("User-Agent") String userAgent,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<TokenResponse> result = registerUserUseCase.execute(dto, ipAddress, userAgent);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "User registered successfully"))
                .build();
    }

    @POST
    @Path("/login")
    @Idempotent
    @RunOnVirtualThread
    public Response login(
            @Valid LoginUserDTO dto,
            @HeaderParam("X-Forwarded-For") String ipAddress,
            @HeaderParam("User-Agent") String userAgent,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<TokenResponse> result = loginUserUseCase.execute(dto, ipAddress, userAgent);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "User authenticated successfully"))
                .build();
    }

    @POST
    @Path("/refresh")
    @Idempotent
    @RunOnVirtualThread
    public Response refresh(
            @Valid RefreshTokenRequestDTO dto,
            @HeaderParam("X-Forwarded-For") String ipAddress,
            @HeaderParam("User-Agent") String userAgent,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<TokenResponse> result = refreshTokenUseCase.execute(dto, ipAddress, userAgent);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(result.getData(), idempotencyKey, "Token refreshed successfully"))
                .build();
    }

    @POST
    @Path("/logout")
    @RunOnVirtualThread
    public Response logout(
            @Valid RefreshTokenRequestDTO dto,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        Result<Void> result = logoutUseCase.execute(dto != null ? dto.refreshToken() : null);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "Logged out successfully"))
                .build();
    }

    @POST
    @Path("/logout-all")
    @Authenticated
    @RunOnVirtualThread
    public Response logoutAll(
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ResponseHTTP.error("Invalid token subject"))
                    .build();
        }

        Long userId = Long.valueOf(sub);
        Result<Void> result = revokeAllUserAccessUseCase.execute(userId);

        if (result.isFailure()) {
            return Response.status(result.getStatus())
                    .entity(ResponseHTTP.error(result.getFirstError()))
                    .build();
        }

        return Response.status(result.getStatus())
                .entity(ResponseHTTP.ok(null, idempotencyKey, "All sessions and tokens revoked successfully"))
                .build();
    }
}
