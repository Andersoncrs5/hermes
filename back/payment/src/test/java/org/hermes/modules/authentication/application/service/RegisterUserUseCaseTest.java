package org.hermes.modules.authentication.application.service;

import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.modules.identity.application.dto.CreateUserDTO;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.ports.out.UserPasswordRepository;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.domain.security.PasswordEncoderPort;
import org.hermes.shared.infrastructure.properties.JwtProperties;
import org.hermes.shared.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterUserUseCaseTest {

    private UserRepository userRepository;
    private UserPasswordRepository userPasswordRepository;
    private PasswordEncoderPort passwordEncoder;
    private ITokenService tokenService;
    private SessionRepository sessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private UserMapper userMapper;
    private JwtProperties jwtProperties;
    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userPasswordRepository = mock(UserPasswordRepository.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        tokenService = mock(ITokenService.class);
        sessionRepository = mock(SessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userMapper = mock(UserMapper.class);
        jwtProperties = mock(JwtProperties.class);

        when(jwtProperties.sessionInactivitySeconds()).thenReturn(86400L);
        when(jwtProperties.refreshTokenExpirationSeconds()).thenReturn(604800L);
        when(jwtProperties.accessTokenExpirationSeconds()).thenReturn(900L);

        useCase = new RegisterUserUseCase(
                userRepository,
                userPasswordRepository,
                passwordEncoder,
                tokenService,
                sessionRepository,
                refreshTokenRepository,
                userMapper,
                jwtProperties
        );
    }

    @Test
    @DisplayName("Should successfully register user and return token response")
    void shouldRegisterUserSuccessfully() {
        CreateUserDTO dto = new CreateUserDTO("novo@hermes.io", "novousuario", "Novo Usuario", "SenhaForte123!");

        when(userRepository.existsByEmail("novo@hermes.io")).thenReturn(false);
        when(userRepository.existsByUsername("novousuario")).thenReturn(false);

        UserEntity user = new UserEntity();
        user.setId(777L);
        user.setEmail("novo@hermes.io");
        user.setUsername("novousuario");

        when(userMapper.toEntity(dto)).thenReturn(user);
        when(userRepository.insert(any(UserEntity.class))).thenReturn(user);
        when(passwordEncoder.encode("SenhaForte123!")).thenReturn("$argon2id$mockedhash");
        when(sessionRepository.insert(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.insert(any())).thenAnswer(i -> i.getArgument(0));

        when(tokenService.generateRefreshTokenValue()).thenReturn("mocked_refresh_token_value_64_characters_long_0123456789abcdef0123");
        when(tokenService.hashToken(anyString())).thenReturn("mocked_hash_value_64_characters_long_0123456789abcdef0123456789abcd");
        when(tokenService.generateToken(any(), any(), any(), any())).thenReturn(Result.success("jwt.mock.token"));

        UserDTO userDto = new UserDTO(777L, "novo@hermes.io", "novousuario", "Novo Usuario", true, false, null, null);
        when(userMapper.toDTO(user)).thenReturn(userDto);

        Result<TokenResponse> result = useCase.execute(dto, "127.0.0.1", "JUnit5");

        assertTrue(result.isSuccess());
        TokenResponse response = result.getData();
        assertEquals("jwt.mock.token", response.token());
        assertEquals("novo@hermes.io", response.user().email());
        verify(userPasswordRepository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should reject registration when email already exists")
    void shouldRejectWhenEmailExists() {
        CreateUserDTO dto = new CreateUserDTO("duplicado@hermes.io", "usuario1", "Nome", "SenhaForte123!");
        when(userRepository.existsByEmail("duplicado@hermes.io")).thenReturn(true);

        Result<TokenResponse> result = useCase.execute(dto, "127.0.0.1", "JUnit5");

        assertTrue(result.isFailure());
        assertEquals(409, result.getStatus());
        verify(userRepository, never()).insert(any());
    }
}
