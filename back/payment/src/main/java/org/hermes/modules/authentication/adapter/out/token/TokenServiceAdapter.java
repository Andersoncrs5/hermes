package org.hermes.modules.authentication.adapter.out.token;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.shared.exception.InternalServerErrorException;
import org.hermes.shared.infrastructure.properties.JwtProperties;
import org.hermes.shared.result.Result;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@ApplicationScoped
public class TokenServiceAdapter implements ITokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceAdapter.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JWTParser parser;
    private final JwtProperties properties;
    private PrivateKey cachedPrivateKey;

    @Inject
    public TokenServiceAdapter(JWTParser parser, JwtProperties properties) {
        this.parser = parser;
        this.properties = properties;
    }

    @Override
    public Result<String> generateToken(UserEntity user, List<String> roles, List<String> permissions, Long tenantId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and ID are required to generate a token.");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            return Result.forb("User account is inactive");
        }

        try {
            JwtClaims claims = new JwtClaims();
            claims.setSubject(user.getId().toString());
            claims.setClaim(Claims.nickname.name(), user.getUsername());
            claims.setClaim(Claims.email.name(), user.getEmail());
            claims.setIssuer(properties.issuer());
            claims.setAudience(properties.audience());
            claims.setIssuedAt(NumericDate.fromSeconds(System.currentTimeMillis() / 1000));
            claims.setGeneratedJwtId();

            long expirationSeconds = properties.accessTokenExpirationSeconds();
            claims.setExpirationTime(NumericDate.fromSeconds((System.currentTimeMillis() / 1000) + expirationSeconds));

            Set<String> groups = new HashSet<>();
            if (roles != null) {
                groups.addAll(roles);
            }
            if (groups.isEmpty()) {
                groups.add("USER");
            }
            claims.setClaim(Claims.groups.name(), groups);

            claims.setClaim("permissions", permissions != null ? permissions : Collections.emptyList());

            if (tenantId != null) {
                claims.setClaim("tenant_id", tenantId.toString());
            }

            claims.setClaim("type", "access");

            String token = signClaims(claims);
            return Result.success(token);
        } catch (Exception e) {
            log.error("Error generating RSA JWT token", e);
            throw new InternalServerErrorException("Failed to generate access token: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> extractAllClaims(String token) {
        try {
            JsonWebToken parsedToken = parseToken(token);
            Map<String, Object> claims = new HashMap<>();
            for (String name : parsedToken.getClaimNames()) {
                claims.put(name, parsedToken.getClaim(name));
            }
            return Result.success(claims);
        } catch (Exception e) {
            return Result.unauthorized("Invalid token claims: " + e.getMessage());
        }
    }

    @Override
    public Result<String> validateToken(String token) {
        try {
            JsonWebToken parsedToken = parseToken(token);
            return Result.success(parsedToken.getSubject());
        } catch (Exception e) {
            return Result.unauthorized("Token validation failed: " + e.getMessage());
        }
    }

    @Override
    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    @Override
    public String hashToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerErrorException("SHA-256 algorithm not available", e);
        }
    }

    private JsonWebToken parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Token missing");
        }
        try {
            return parser.parse(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token: " + e.getMessage());
        }
    }

    private String signClaims(JwtClaims claims) throws Exception {
        PrivateKey privateKey = getPrivateKey();
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(privateKey);
        jws.setHeader("typ", "JWT");
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws.getCompactSerialization();
    }

    private synchronized PrivateKey getPrivateKey() throws Exception {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }

        try (InputStream is = getClass().getResourceAsStream("/privateKey.pem")) {
            if (is == null) {
                throw new IllegalStateException("Private key not found at /privateKey.pem");
            }
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\r", "")
                    .replaceAll("\\n", "")
                    .trim();

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            cachedPrivateKey = keyFactory.generatePrivate(spec);
            return cachedPrivateKey;
        }
    }
}
