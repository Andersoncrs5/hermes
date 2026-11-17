package org.hermes.modules.authentication.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authentication.application.dto.LoginUserDTO;
import org.hermes.modules.authentication.domain.entity.LoginAttemptEntity;
import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.modules.authentication.domain.entity.SessionEntity;
import org.hermes.modules.authentication.ports.in.ILoginUserUseCase;
import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.authentication.ports.out.LoginAttemptRepository;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.modules.authorization.domain.entity.PermissionEntity;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.domain.entity.UserPasswordEntity;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.out.UserPasswordRepository;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.domain.security.PasswordEncoderPort;
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
public class LoginUserUseCase implements ILoginUserUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final ITokenService tokenService;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserTenantRepository userTenantRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    @Inject
    public LoginUserUseCase(
            UserRepository userRepository,
            UserPasswordRepository userPasswordRepository,
            LoginAttemptRepository loginAttemptRepository,
            PasswordEncoderPort passwordEncoder,
            ITokenService tokenService,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserTenantRepository userTenantRepository,
            UserTenantRoleRepository userTenantRoleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserMapper userMapper,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.userPasswordRepository = userPasswordRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userTenantRepository = userTenantRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @ResultTransaction
    public Result<TokenResponse> execute(LoginUserDTO dto, String ipAddress, String userAgent) {
        if (dto == null || dto.identifier() == null || dto.password() == null) {
            return Result.failure("Invalid login credentials", 400);
        }

        OffsetDateTime window = OffsetDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        String identifier = dto.identifier().trim();

        if (ipAddress != null) {
            long failedIp = loginAttemptRepository.countRecentFailedAttemptsByIp(ipAddress, window);
            if (failedIp >= MAX_FAILED_ATTEMPTS * 2) {
                recordAttempt(identifier, ipAddress, userAgent, false, "IP_RATE_LIMITED");
                return Result.failure("IP temporariamente bloqueado devido a excesso de tentativas falhas", 429);
            }
        }

        long failedUser = loginAttemptRepository.countRecentFailedAttemptsByUsername(identifier, window);
        if (failedUser >= MAX_FAILED_ATTEMPTS) {
            recordAttempt(identifier, ipAddress, userAgent, false, "USER_LOCKOUT");
            return Result.failure("Conta temporariamente bloqueada. Tente novamente mais tarde.", 423);
        }

        Optional<UserEntity> userOpt = identifier.contains("@")
                ? userRepository.findByEmail(identifier.toLowerCase())
                : userRepository.findByUsername(identifier.toLowerCase());

        if (userOpt.isEmpty()) {
            recordAttempt(identifier, ipAddress, userAgent, false, "USER_NOT_FOUND");
            return Result.unauthorized("Credenciais inválidas");
        }

        UserEntity user = userOpt.get();

        if (Boolean.FALSE.equals(user.getIsActive()) || user.isDeleted()) {
            recordAttempt(identifier, ipAddress, userAgent, false, "USER_INACTIVE");
            return Result.forb("Conta de usuário inativa");
        }

        Optional<UserPasswordEntity> pwdOpt = userPasswordRepository.findByUserId(user.getId());
        if (pwdOpt.isEmpty()) {
            recordAttempt(identifier, ipAddress, userAgent, false, "NO_CREDENTIALS");
            return Result.unauthorized("Credenciais inválidas");
        }

        UserPasswordEntity userPassword = pwdOpt.get();
        boolean passwordMatches = passwordEncoder.matches(dto.password(), userPassword.getPasswordHash());

        if (!passwordMatches) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            recordAttempt(identifier, ipAddress, userAgent, false, "INVALID_PASSWORD");
            return Result.unauthorized("Credenciais inválidas");
        }

        // Login success
        user.resetFailedAttempts();
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        recordAttempt(identifier, ipAddress, userAgent, true, null);

        // Gather roles and permissions across active tenants
        Set<String> roles = new HashSet<>();
        roles.add("user");
        List<Long> roleIds = new ArrayList<>();

        List<UserTenantEntity> memberships = userTenantRepository.findByUserId(user.getId());
        Long primaryTenantId = null;

        for (UserTenantEntity membership : memberships) {
            if (membership.isActive()) {
                if (primaryTenantId == null) {
                    primaryTenantId = membership.getTenantId();
                }
                List<UserTenantRoleEntity> utRoles = userTenantRoleRepository.findByUserTenantId(membership.getId());
                for (UserTenantRoleEntity utRole : utRoles) {
                    roleIds.add(utRole.getRoleId());
                    roleRepository.findById(utRole.getRoleId()).ifPresent(r -> roles.add(r.getSlug()));
                }
            }
        }

        Set<String> permissions = new HashSet<>();
        permissions.add("iam.profile.read");
        permissions.add("iam.profile.update");

        if (!roleIds.isEmpty()) {
            List<PermissionEntity> perms = permissionRepository.findByRoleIds(roleIds);
            for (PermissionEntity p : perms) {
                permissions.add(p.getSlug());
            }
        }

        // Create Session
        SessionEntity session = new SessionEntity();
        session.setUserId(user.getId());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.sessionInactivitySeconds()));
        session = sessionRepository.insert(session);

        // Create Refresh Token
        String rawRefreshToken = tokenService.generateRefreshTokenValue();
        String hashedRefreshToken = tokenService.hashToken(rawRefreshToken);
        OffsetDateTime refreshExp = OffsetDateTime.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setSessionId(session.getId());
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(hashedRefreshToken);
        refreshToken.setExpiredAt(refreshExp);
        refreshTokenRepository.insert(refreshToken);

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
                rawRefreshToken,
                refreshExp,
                userMapper.toDTO(user),
                rolesList
        );

        return Result.success(tokenResponse);
    }

    private void recordAttempt(String username, String ip, String userAgent, boolean success, String reason) {
        LoginAttemptEntity attempt = new LoginAttemptEntity();
        attempt.setUsername(username);
        attempt.setIpAddress(ip);
        attempt.setUserAgent(userAgent);
        attempt.setSuccess(success);
        attempt.setFailureReason(reason);
        attempt.setAttemptAt(OffsetDateTime.now());
        loginAttemptRepository.save(attempt);
    }
}
