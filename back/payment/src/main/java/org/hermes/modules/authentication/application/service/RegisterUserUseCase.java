package org.hermes.modules.authentication.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.modules.authentication.domain.entity.SessionEntity;
import org.hermes.modules.identity.domain.entity.UserPasswordEntity;
import org.hermes.modules.authentication.ports.in.IRegisterUserUseCase;
import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.modules.identity.ports.out.UserPasswordRepository;
import org.hermes.modules.identity.application.dto.CreateUserDTO;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.domain.security.PasswordEncoderPort;
import org.hermes.shared.infrastructure.properties.JwtProperties;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class RegisterUserUseCase implements IRegisterUserUseCase {

    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final ITokenService tokenService;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    @Inject
    public RegisterUserUseCase(
            UserRepository userRepository,
            UserPasswordRepository userPasswordRepository,
            PasswordEncoderPort passwordEncoder,
            ITokenService tokenService,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserMapper userMapper,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.userPasswordRepository = userPasswordRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @ResultTransaction
    public Result<TokenResponse> execute(CreateUserDTO dto, String ipAddress, String userAgent) {
        if (dto == null) {
            return Result.failure("Invalid user data", 400);
        }

        String email = dto.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return Result.conflict("Email '" + email + "' is already registered");
        }

        String username = dto.username().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            return Result.conflict("Username '" + username + "' is already taken");
        }

        UserEntity user = userMapper.toEntity(dto);
        user.setEmail(email);
        user.setUsername(username);
        user = userRepository.insert(user);

        String hash = passwordEncoder.encode(dto.password());
        UserPasswordEntity passwordEntity = new UserPasswordEntity();
        passwordEntity.setUserId(user.getId());
        passwordEntity.setPasswordHash(hash);
        passwordEntity.setTypeHash("argon2id");
        passwordEntity.setShouldUpdate(false);
        userPasswordRepository.insert(passwordEntity);

        // Session
        SessionEntity session = new SessionEntity();
        session.setUserId(user.getId());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.sessionInactivitySeconds()));
        session = sessionRepository.insert(session);

        // Refresh token
        String rawRefreshToken = tokenService.generateRefreshTokenValue();
        String hashedRefreshToken = tokenService.hashToken(rawRefreshToken);
        OffsetDateTime refreshExp = OffsetDateTime.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setSessionId(session.getId());
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(hashedRefreshToken);
        refreshToken.setExpiredAt(refreshExp);
        refreshTokenRepository.insert(refreshToken);

        List<String> defaultRoles = List.of("user");
        List<String> defaultPerms = List.of("iam.profile.read", "iam.profile.update");

        Result<String> tokenResult = tokenService.generateToken(user, defaultRoles, defaultPerms, null);
        if (tokenResult.isFailure()) {
            return Result.failure(tokenResult.getFirstError(), tokenResult.getStatus());
        }

        OffsetDateTime tokenExp = OffsetDateTime.now().plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        TokenResponse response = new TokenResponse(
                tokenResult.getData(),
                tokenExp,
                rawRefreshToken,
                refreshExp,
                userMapper.toDTO(user),
                defaultRoles
        );

        return Result.success(response);
    }
}
