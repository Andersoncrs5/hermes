package org.hermes.modules.identity.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.modules.identity.application.dto.CreateTenantDTO;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.modules.identity.application.mapper.TenantMapper;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.in.ICreateTenantUseCase;
import org.hermes.modules.identity.ports.out.TenantRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class CreateTenantUseCase implements ICreateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final RoleRepository roleRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final TenantMapper tenantMapper;

    @Inject
    public CreateTenantUseCase(
            TenantRepository tenantRepository,
            UserTenantRepository userTenantRepository,
            RoleRepository roleRepository,
            UserTenantRoleRepository userTenantRoleRepository,
            TenantMapper tenantMapper
    ) {
        this.tenantRepository = tenantRepository;
        this.userTenantRepository = userTenantRepository;
        this.roleRepository = roleRepository;
        this.userTenantRoleRepository = userTenantRoleRepository;
        this.tenantMapper = tenantMapper;
    }

    @Override
    @ResultTransaction
    public Result<TenantDTO> execute(CreateTenantDTO dto, Long creatorUserId) {
        if (dto == null || dto.slug() == null || dto.name() == null) {
            return Result.failure("Invalid tenant data", 400);
        }

        String normalizedSlug = dto.slug().trim().toLowerCase();
        Optional<TenantEntity> existing = tenantRepository.findBySlug(normalizedSlug);
        if (existing.isPresent()) {
            return Result.conflict("Tenant with slug '" + normalizedSlug + "' already exists");
        }

        TenantEntity tenant = tenantMapper.toEntity(dto);
        tenant.setSlug(normalizedSlug);
        tenant.setCountryCode(dto.countryCode() != null ? dto.countryCode().toUpperCase() : "BR");
        tenant = tenantRepository.insert(tenant);

        if (creatorUserId != null) {
            UserTenantEntity userTenant = new UserTenantEntity();
            userTenant.setUserId(creatorUserId);
            userTenant.setTenantId(tenant.getId());
            userTenant.setStatus("ACTIVE");
            userTenant.setJoinedAt(OffsetDateTime.now());
            userTenant = userTenantRepository.insert(userTenant);

            Optional<RoleEntity> adminRole = roleRepository.findBySlug("admin");
            if (adminRole.isEmpty()) {
                adminRole = roleRepository.findBySlug("owner");
            }

            if (adminRole.isPresent()) {
                UserTenantRoleEntity utr = new UserTenantRoleEntity();
                utr.setUserTenantId(userTenant.getId());
                utr.setRoleId(adminRole.get().getId());
                userTenantRoleRepository.insert(utr);
            }
        }

        return Result.success(tenantMapper.toDTO(tenant));
    }
}
