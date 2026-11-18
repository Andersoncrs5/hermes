package org.hermes.modules.apikey.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.apikey.application.dto.ApiKeyResponseDTO;
import org.hermes.modules.apikey.application.dto.CreateApiKeyDTO;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.modules.apikey.domain.entity.ApiKeyPermissionEntity;
import org.hermes.modules.apikey.ports.in.ICreateApiKeyUseCase;
import org.hermes.modules.apikey.ports.out.ApiKeyPermissionRepository;
import org.hermes.modules.apikey.ports.out.ApiKeyRepository;
import org.hermes.shared.exception.InternalServerErrorException;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@ApplicationScoped
public class CreateApiKeyUseCase implements ICreateApiKeyUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository apiKeyPermissionRepository;

    @Inject
    public CreateApiKeyUseCase(
            ApiKeyRepository apiKeyRepository,
            ApiKeyPermissionRepository apiKeyPermissionRepository
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyPermissionRepository = apiKeyPermissionRepository;
    }

    @Override
    @ResultTransaction
    public Result<ApiKeyResponseDTO> execute(Long tenantId, Long userId, CreateApiKeyDTO dto) {
        if (tenantId == null || dto == null || dto.name() == null || dto.name().isBlank()) {
            return Result.failure("Invalid API key data", 400);
        }

        String env = "LIVE".equalsIgnoreCase(dto.environment()) ? "LIVE" : "TEST";
        String tag = "LIVE".equals(env) ? "hm_live_" : "hm_test_"; // 8 chars

        byte[] prefixBytes = new byte[4];
        SECURE_RANDOM.nextBytes(prefixBytes);
        String prefixRandom = HexFormat.of().formatHex(prefixBytes); // 8 chars
        String keyPrefix = tag + prefixRandom; // Exactly 16 chars

        byte[] secretBytes = new byte[24];
        SECURE_RANDOM.nextBytes(secretBytes);
        String secretRandom = HexFormat.of().formatHex(secretBytes); // 48 chars
        String fullKey = keyPrefix + "_" + secretRandom;

        String secretHash = hashSecret(fullKey);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setTenantId(tenantId);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setSecretHash(secretHash);
        apiKey.setName(dto.name().trim());
        apiKey.setEnvironment(env);
        apiKey.setStatus("ACTIVE");
        apiKey.setCreatedBy(userId);
        apiKey = apiKeyRepository.insert(apiKey);

        if (dto.permissionIds() != null) {
            for (Long permId : dto.permissionIds()) {
                ApiKeyPermissionEntity akp = new ApiKeyPermissionEntity();
                akp.setApiKeyId(apiKey.getId());
                akp.setPermissionId(permId);
                apiKeyPermissionRepository.insert(akp);
            }
        }

        ApiKeyResponseDTO response = new ApiKeyResponseDTO(
                apiKey.getId(),
                apiKey.getTenantId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                fullKey,
                apiKey.getEnvironment(),
                apiKey.getExpiredAt(),
                apiKey.getCreatedAt()
        );

        return Result.success(response);
    }

    private String hashSecret(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerErrorException("SHA-256 algorithm not available", e);
        }
    }
}
