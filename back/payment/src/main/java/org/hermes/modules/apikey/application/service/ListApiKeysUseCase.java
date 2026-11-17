package org.hermes.modules.apikey.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.apikey.application.dto.ApiKeyDTO;
import org.hermes.modules.apikey.application.mapper.ApiKeyMapper;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.modules.apikey.ports.in.IListApiKeysUseCase;
import org.hermes.modules.apikey.ports.out.ApiKeyRepository;
import org.hermes.shared.result.Result;

import java.util.List;

@ApplicationScoped
public class ListApiKeysUseCase implements IListApiKeysUseCase {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    @Inject
    public ListApiKeysUseCase(ApiKeyRepository apiKeyRepository, ApiKeyMapper apiKeyMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyMapper = apiKeyMapper;
    }

    @Override
    public Result<List<ApiKeyDTO>> execute(Long tenantId) {
        if (tenantId == null) {
            return Result.failure("Tenant ID is required", 400);
        }

        List<ApiKeyEntity> keys = apiKeyRepository.findByTenantId(tenantId);
        return Result.success(apiKeyMapper.toDTO(keys));
    }
}
