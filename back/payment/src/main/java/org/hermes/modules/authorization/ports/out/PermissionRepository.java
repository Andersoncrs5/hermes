package org.hermes.modules.authorization.ports.out;

import org.hermes.modules.authorization.domain.entity.PermissionEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends GenericRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<PermissionEntity> findByModule(String module);

    List<PermissionEntity> findByRoleIds(List<Long> roleIds);

    List<PermissionEntity> findDirectPermissionsByUserTenantId(Long userTenantId);
}
