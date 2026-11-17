package org.hermes.modules.identity.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.application.dto.InviteUserToTenantDTO;
import org.hermes.modules.identity.application.dto.UserTenantDTO;
import org.hermes.modules.identity.application.mapper.UserTenantMapper;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.in.IInviteUserToTenantUseCase;
import org.hermes.modules.identity.ports.out.TenantRepository;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class InviteUserToTenantUseCase implements IInviteUserToTenantUseCase {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final RoleRepository roleRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final UserTenantMapper userTenantMapper;

    @Inject
    public InviteUserToTenantUseCase(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            UserTenantRepository userTenantRepository,
            RoleRepository roleRepository,
            UserTenantRoleRepository userTenantRoleRepository,
            UserTenantMapper userTenantMapper
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.roleRepository = roleRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.userTenantMapper = userTenantMapper;
    }

    @Override
    @ResultTransaction
    public Result<UserTenantDTO> execute(Long tenantId, InviteUserToTenantDTO dto, Long inviterUserId) {
        if (tenantId == null || dto == null || dto.email() == null) {
            return Result.failure("Invalid invitation data", 400);
        }

        Optional<TenantEntity> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty() || tenantOpt.get().isDeleted()) {
            return Result.notFound("Tenant not found");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(dto.email().trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return Result.notFound("User with email '" + dto.email() + "' not found. User must be registered first.");
        }

        UserEntity user = userOpt.get();
        Optional<UserTenantEntity> existingLink = userTenantRepository.findByUserAndTenant(user.getId(), tenantId);
        if (existingLink.isPresent()) {
            UserTenantEntity ut = existingLink.get();
            if (ut.isActive()) {
                return Result.conflict("User is already an active member of this tenant");
            }
            ut.setStatus("ACTIVE");
            ut.setJoinedAt(OffsetDateTime.now());
            ut.setInvitedBy(inviterUserId);
            userTenantRepository.save(ut);
            return Result.success(userTenantMapper.toDTO(ut));
        }

        UserTenantEntity newLink = new UserTenantEntity();
        newLink.setUserId(user.getId());
        newLink.setTenantId(tenantId);
        newLink.setStatus("ACTIVE");
        newLink.setJoinedAt(OffsetDateTime.now());
        newLink.setInvitedBy(inviterUserId);
        newLink = userTenantRepository.insert(newLink);

        String targetRole = dto.roleName() != null ? dto.roleName().toLowerCase() : "user";
        Optional<RoleEntity> roleOpt = roleRepository.findBySlug(targetRole);
        if (roleOpt.isPresent()) {
            UserTenantRoleEntity utr = new UserTenantRoleEntity();
            utr.setUserTenantId(newLink.getId());
            utr.setRoleId(roleOpt.get().getId());
            userTenantRoleRepository.insert(utr);
        }

        return Result.success(userTenantMapper.toDTO(newLink));
    }
}
