package org.hermes.modules.authorization.ports.out;

import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface UserTenantRoleRepository extends GenericRepository<UserTenantRoleEntity, Long> {

    List<UserTenantRoleEntity> findByUserTenantId(Long userTenantId);

    Optional<UserTenantRoleEntity> findByUserTenantAndRole(Long userTenantId, Long roleId);

    int deleteByUserTenantAndRole(Long userTenantId, Long roleId);
}
