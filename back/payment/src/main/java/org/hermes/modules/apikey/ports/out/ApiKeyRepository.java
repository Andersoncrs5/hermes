package org.hermes.modules.apikey.ports.out;

import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends GenericRepository<ApiKeyEntity, Long> {

    Optional<ApiKeyEntity> findByPrefix(String keyPrefix);

    List<ApiKeyEntity> findByTenantId(Long tenantId);
}
