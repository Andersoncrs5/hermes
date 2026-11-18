package org.hermes.modules.authorization.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.domain.entity.PermissionEntity;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.RolePermissionEntity;
import org.hermes.modules.authorization.ports.in.IAssignPermissionToRoleUseCase;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.modules.authorization.ports.out.RolePermissionRepository;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class AssignPermissionToRoleUseCase implements IAssignPermissionToRoleUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Inject
    public AssignPermissionToRoleUseCase(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            return Result.failure("Role ID and Permission ID are required", 400);
        }

        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || roleOpt.get().isDeleted()) {
            return Result.notFound("Role not found");
        }

        Optional<PermissionEntity> permOpt = permissionRepository.findById(permissionId);
        if (permOpt.isEmpty() || permOpt.get().isDeleted()) {
            return Result.notFound("Permission not found");
        }

        Optional<RolePermissionEntity> existing = rolePermissionRepository.findByRoleAndPermission(roleId, permissionId);
        if (existing.isPresent()) {
            return Result.conflict("Permission is already assigned to this role");
        }

        RolePermissionEntity rp = new RolePermissionEntity();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionRepository.insert(rp);

        return Result.success(null);
    }
}
