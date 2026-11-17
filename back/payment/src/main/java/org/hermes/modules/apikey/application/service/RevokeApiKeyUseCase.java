package org.hermes.modules.apikey.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.modules.apikey.ports.in.IRevokeApiKeyUseCase;
import org.hermes.modules.apikey.ports.out.ApiKeyRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class RevokeApiKeyUseCase implements IRevokeApiKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;

    @Inject
    public RevokeApiKeyUseCase(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(Long tenantId, Long apiKeyId) {
        if (tenantId == null || apiKeyId == null) {
            return Result.failure("Tenant ID and API key ID are required", 400);
        }

        Optional<ApiKeyEntity> keyOpt = apiKeyRepository.findById(apiKeyId);
        if (keyOpt.isEmpty() || keyOpt.get().isDeleted() || !tenantId.equals(keyOpt.get().getTenantId())) {
            return Result.notFound("API key not found");
        }

        ApiKeyEntity apiKey = keyOpt.get();
        apiKey.revoke();
        apiKeyRepository.save(apiKey);

        return Result.success(null);
    }
}
