package org.hermes.modules.identity.ports.out;

import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.Optional;

public interface TenantRepository extends GenericRepository<TenantEntity, Long> {

    Optional<TenantEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
