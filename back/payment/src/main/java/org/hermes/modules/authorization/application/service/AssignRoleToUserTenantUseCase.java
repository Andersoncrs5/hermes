package org.hermes.modules.authorization.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.application.dto.AssignRoleToUserTenantDTO;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.in.IAssignRoleToUserTenantUseCase;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class AssignRoleToUserTenantUseCase implements IAssignRoleToUserTenantUseCase {

    private final UserTenantRepository userTenantRepository;
    private final RoleRepository roleRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;

    @Inject
    public AssignRoleToUserTenantUseCase(
            UserTenantRepository userTenantRepository,
            RoleRepository roleRepository,
            UserTenantRoleRepository userTenantRoleRepository
    ) {
        this.userTenantRepository = userTenantRepository;
        this.roleRepository = roleRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(AssignRoleToUserTenantDTO dto) {
        if (dto == null || dto.userTenantId() == null || dto.roleId() == null) {
            return Result.failure("Invalid assignment data", 400);
        }

        Optional<UserTenantEntity> utOpt = userTenantRepository.findById(dto.userTenantId());
        if (utOpt.isEmpty() || utOpt.get().isDeleted()) {
            return Result.notFound("UserTenant membership not found");
        }

        Optional<RoleEntity> roleOpt = roleRepository.findById(dto.roleId());
        if (roleOpt.isEmpty() || roleOpt.get().isDeleted()) {
            return Result.notFound("Role not found");
        }

        Optional<UserTenantRoleEntity> existing = userTenantRoleRepository.findByUserTenantAndRole(dto.userTenantId(), dto.roleId());
        if (existing.isPresent()) {
            return Result.conflict("Role is already assigned to this user in tenant");
        }

        UserTenantRoleEntity utr = new UserTenantRoleEntity();
        utr.setUserTenantId(dto.userTenantId());
        utr.setRoleId(dto.roleId());
        userTenantRoleRepository.insert(utr);

        return Result.success(null);
    }
}
