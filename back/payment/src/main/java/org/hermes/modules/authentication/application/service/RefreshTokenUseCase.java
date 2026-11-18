package org.hermes.modules.authentication.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authentication.application.dto.RefreshTokenRequestDTO;
import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.modules.authentication.ports.in.IRefreshTokenUseCase;
import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authorization.domain.entity.RolePermissionEntity;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.modules.authorization.ports.out.RolePermissionRepository;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.infrastructure.properties.JwtProperties;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class RefreshTokenUseCase implements IRefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final ITokenService tokenService;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    @Inject
    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            UserTenantRepository userTenantRepository,
            UserTenantRoleRepository userTenantRoleRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            ITokenService tokenService,
            UserMapper userMapper,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.tokenService = tokenService;
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @ResultTransaction
    public Result<TokenResponse> execute(RefreshTokenRequestDTO dto, String ipAddress, String userAgent) {
        if (dto == null || dto.refreshToken() == null || dto.refreshToken().isBlank()) {
            return Result.failure("Refresh token is required", 400);
        }

        String tokenHash = tokenService.hashToken(dto.refreshToken());
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return Result.unauthorized("Invalid refresh token");
        }

        RefreshTokenEntity currentToken = tokenOpt.get();
        if (!currentToken.isValid()) {
            return Result.unauthorized("Refresh token is expired or revoked");
        }

        Optional<UserEntity> userOpt = userRepository.findById(currentToken.getUserId());
        if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getIsActive()) || userOpt.get().isDeleted()) {
            return Result.unauthorized("User account is inactive or not found");
        }

        UserEntity user = userOpt.get();

        // Rotate refresh token: revoke current
        currentToken.revoke();
        refreshTokenRepository.save(currentToken);

        // Generate new refresh token
        String rawNewRefreshToken = tokenService.generateRefreshTokenValue();
        String hashedNewRefreshToken = tokenService.hashToken(rawNewRefreshToken);
        OffsetDateTime refreshExp = OffsetDateTime.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        RefreshTokenEntity newRefreshToken = new RefreshTokenEntity();
        newRefreshToken.setSessionId(currentToken.getSessionId());
        newRefreshToken.setUserId(user.getId());
        newRefreshToken.setTokenHash(hashedNewRefreshToken);
        newRefreshToken.setExpiredAt(refreshExp);
        refreshTokenRepository.insert(newRefreshToken);

        // Gather roles and permissions
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        roles.add("user");
        permissions.add("iam.profile.read");
        permissions.add("iam.profile.update");

        List<UserTenantEntity> memberships = userTenantRepository.findByUserId(user.getId());
        Long primaryTenantId = null;

        for (UserTenantEntity membership : memberships) {
            if (membership.isActive()) {
                if (primaryTenantId == null) {
                    primaryTenantId = membership.getTenantId();
                }
                List<UserTenantRoleEntity> utRoles = userTenantRoleRepository.findByUserTenantId(membership.getId());
                for (UserTenantRoleEntity utRole : utRoles) {
                    roleRepository.findById(utRole.getRoleId()).ifPresent(r -> {
                        roles.add(r.getName());
                        List<RolePermissionEntity> rolePerms = rolePermissionRepository.findByRoleId(r.getId());
                        for (RolePermissionEntity rp : rolePerms) {
                            permissionRepository.findById(rp.getPermissionId()).ifPresent(p -> permissions.add(p.getName()));
                        }
                    });
                }
            }
        }

        List<String> rolesList = new ArrayList<>(roles);
        List<String> permissionsList = new ArrayList<>(permissions);

        Result<String> tokenResult = tokenService.generateToken(user, rolesList, permissionsList, primaryTenantId);
        if (tokenResult.isFailure()) {
            return Result.failure(tokenResult.getFirstError(), tokenResult.getStatus());
        }

        OffsetDateTime tokenExp = OffsetDateTime.now().plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        TokenResponse tokenResponse = new TokenResponse(
                tokenResult.getData(),
                tokenExp,
                rawNewRefreshToken,
                refreshExp,
                userMapper.toDTO(user),
                rolesList
        );

        return Result.success(tokenResponse);
    }
}
