package org.hermes.modules.identity.ports.out;

import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface UserTenantRepository extends GenericRepository<UserTenantEntity, Long> {

    Optional<UserTenantEntity> findByUserAndTenant(Long userId, Long tenantId);

    List<UserTenantEntity> findByUserId(Long userId);

    List<UserTenantEntity> findByTenantId(Long tenantId);

    boolean existsByUserAndTenant(Long userId, Long tenantId);
}
