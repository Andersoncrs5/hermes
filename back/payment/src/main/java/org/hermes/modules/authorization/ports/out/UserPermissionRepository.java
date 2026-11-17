package org.hermes.modules.authorization.ports.out;

import org.hermes.modules.authorization.domain.entity.UserPermissionEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository extends GenericRepository<UserPermissionEntity, Long> {

    List<UserPermissionEntity> findByUserTenantId(Long userTenantId);

    Optional<UserPermissionEntity> findByUserTenantAndPermission(Long userTenantId, Long permissionId);

    int deleteByUserTenantAndPermission(Long userTenantId, Long permissionId);
}
