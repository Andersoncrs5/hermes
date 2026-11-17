package org.hermes.modules.apikey.ports.out;

import org.hermes.modules.apikey.domain.entity.ApiKeyPermissionEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;

public interface ApiKeyPermissionRepository extends GenericRepository<ApiKeyPermissionEntity, Long> {

    List<ApiKeyPermissionEntity> findByApiKeyId(Long apiKeyId);

    int deleteByApiKeyId(Long apiKeyId);
}
