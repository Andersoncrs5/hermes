package org.hermes.modules.authentication.application.service;

import org.hermes.modules.authentication.application.dto.LoginUserDTO;
import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.modules.authentication.domain.entity.SessionEntity;
import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.authentication.ports.out.LoginAttemptRepository;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.domain.entity.UserPasswordEntity;
import org.hermes.modules.identity.ports.out.UserPasswordRepository;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.domain.security.PasswordEncoderPort;
import org.hermes.shared.infrastructure.properties.JwtProperties;
import org.hermes.shared.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginUserUseCaseTest {

    private UserRepository userRepository;
    private UserPasswordRepository userPasswordRepository;
    private LoginAttemptRepository loginAttemptRepository;
    private PasswordEncoderPort passwordEncoder;
    private ITokenService tokenService;
    private SessionRepository sessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private UserTenantRepository userTenantRepository;
    private UserTenantRoleRepository userTenantRoleRepository;
    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private UserMapper userMapper;
    private JwtProperties jwtProperties;
    private LoginUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userPasswordRepository = mock(UserPasswordRepository.class);
        loginAttemptRepository = mock(LoginAttemptRepository.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        tokenService = mock(ITokenService.class);
        sessionRepository = mock(SessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userTenantRepository = mock(UserTenantRepository.class);
        userTenantRoleRepository = mock(UserTenantRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        userMapper = mock(UserMapper.class);
        jwtProperties = mock(JwtProperties.class);

        when(jwtProperties.sessionInactivitySeconds()).thenReturn(86400L);
        when(jwtProperties.refreshTokenExpirationSeconds()).thenReturn(604800L);
        when(jwtProperties.accessTokenExpirationSeconds()).thenReturn(900L);

        useCase = new LoginUserUseCase(
                userRepository,
                userPasswordRepository,
                loginAttemptRepository,
                passwordEncoder,
                tokenService,
                sessionRepository,
                refreshTokenRepository,
                userTenantRepository,
                userTenantRoleRepository,
                roleRepository,
                permissionRepository,
                userMapper,
                jwtProperties
        );
    }

    @Test
    @DisplayName("Should login successfully with valid email and password")
    void shouldLoginSuccessfully() {
        LoginUserDTO dto = new LoginUserDTO("admin@hermes.io", "SecretPass123!");

        UserEntity user = new UserEntity();
        user.setId(1001L);
        user.setEmail("admin@hermes.io");
        user.setUsername("admin");
        user.setIsActive(true);

        when(userRepository.findByEmail("admin@hermes.io")).thenReturn(Optional.of(user));

        UserPasswordEntity pwd = new UserPasswordEntity();
        pwd.setUserId(1001L);
        pwd.setPasswordHash("$argon2id$somehash");
        when(userPasswordRepository.findByUserId(1001L)).thenReturn(Optional.of(pwd));

        when(passwordEncoder.matches("SecretPass123!", "$argon2id$somehash")).thenReturn(true);
        when(userTenantRepository.findByUserId(1001L)).thenReturn(Collections.emptyList());

        when(sessionRepository.insert(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.insert(any())).thenAnswer(i -> i.getArgument(0));
        when(tokenService.generateRefreshTokenValue()).thenReturn("token_val_64_characters_long_0123456789abcdef0123456789abcdef01234567");
        when(tokenService.hashToken(anyString())).thenReturn("token_hash_64_characters_long_0123456789abcdef0123456789abcdef012345");
        when(tokenService.generateToken(any(), any(), any(), any())).thenReturn(Result.success("jwt.token.here"));

        UserDTO userDto = new UserDTO(1001L, "admin@hermes.io", "admin", "Admin", true, true, null, null);
        when(userMapper.toDTO(user)).thenReturn(userDto);

        Result<TokenResponse> result = useCase.execute(dto, "10.0.0.1", "Browser");

        assertTrue(result.isSuccess());
        TokenResponse response = result.getData();
        assertEquals("jwt.token.here", response.token());
        assertEquals("admin@hermes.io", response.user().email());
    }

    @Test
    @DisplayName("Should fail with 401 on incorrect password")
    void shouldFailOnIncorrectPassword() {
        LoginUserDTO dto = new LoginUserDTO("admin@hermes.io", "WrongPassword!");

        UserEntity user = new UserEntity();
        user.setId(1001L);
        user.setEmail("admin@hermes.io");
        user.setIsActive(true);

        when(userRepository.findByEmail("admin@hermes.io")).thenReturn(Optional.of(user));

        UserPasswordEntity pwd = new UserPasswordEntity();
        pwd.setUserId(1001L);
        pwd.setPasswordHash("$argon2id$somehash");
        when(userPasswordRepository.findByUserId(1001L)).thenReturn(Optional.of(pwd));

        when(passwordEncoder.matches("WrongPassword!", "$argon2id$somehash")).thenReturn(false);

        Result<TokenResponse> result = useCase.execute(dto, "10.0.0.1", "Browser");

        assertTrue(result.isFailure());
        assertEquals(401, result.getStatus());
    }
}
