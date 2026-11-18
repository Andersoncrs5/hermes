package org.hermes.modules.authentication.adapter.out.token;

import io.smallrye.jwt.auth.principal.JWTParser;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.shared.infrastructure.properties.JwtProperties;
import org.hermes.shared.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TokenServiceAdapterTest {

    private TokenServiceAdapter tokenService;
    private JWTParser parser;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        parser = Mockito.mock(JWTParser.class);
        properties = Mockito.mock(JwtProperties.class);

        when(properties.issuer()).thenReturn("https://hermes.io");
        when(properties.audience()).thenReturn("hermes-api");
        when(properties.accessTokenExpirationSeconds()).thenReturn(900L);
        when(properties.refreshTokenExpirationSeconds()).thenReturn(604800L);
        when(properties.sessionInactivitySeconds()).thenReturn(86400L);

        tokenService = new TokenServiceAdapter(parser, properties);
    }

    @Test
    @DisplayName("Should generate valid RSA JWT access token")
    void shouldGenerateValidAccessToken() {
        UserEntity user = new UserEntity();
        user.setId(123456789012345L);
        user.setUsername("testuser");
        user.setEmail("test@hermes.io");
        user.setIsActive(true);

        List<String> roles = List.of("user", "admin");
        List<String> permissions = List.of("iam.user.read");

        Result<String> result = tokenService.generateToken(user, roles, permissions, 9999L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().startsWith("ey"), "JWT should start with 'ey'");
    }

    @Test
    @DisplayName("Should generate 64-character hex refresh token and valid SHA-256 hash")
    void shouldGenerateRefreshTokenAndHash() {
        String rawToken = tokenService.generateRefreshTokenValue();
        assertNotNull(rawToken);
        assertEquals(64, rawToken.length(), "Raw refresh token must be 32 bytes (64 hex characters)");

        String hash = tokenService.hashToken(rawToken);
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash must be 64 hex characters");

        // Hashing is deterministic
        assertEquals(hash, tokenService.hashToken(rawToken));
    }
}
