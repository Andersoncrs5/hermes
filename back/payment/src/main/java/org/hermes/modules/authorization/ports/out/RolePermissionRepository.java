package org.hermes.modules.authorization.ports.out;

import org.hermes.modules.authorization.domain.entity.RolePermissionEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends GenericRepository<RolePermissionEntity, Long> {

    List<RolePermissionEntity> findByRoleId(Long roleId);

    Optional<RolePermissionEntity> findByRoleAndPermission(Long roleId, Long permissionId);

    int deleteByRoleAndPermission(Long roleId, Long permissionId);
}
