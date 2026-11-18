package org.hermes.modules.identity.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.modules.identity.application.mapper.TenantMapper;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.in.IListTenantsUseCase;
import org.hermes.modules.identity.ports.out.TenantRepository;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ListTenantsUseCase implements IListTenantsUseCase {

    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantMapper tenantMapper;

    @Inject
    public ListTenantsUseCase(
            TenantRepository tenantRepository,
            UserTenantRepository userTenantRepository,
            TenantMapper tenantMapper
    ) {
        this.tenantRepository = tenantRepository;
        this.userTenantRepository = userTenantRepository;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public Result<List<TenantDTO>> execute(Long userId) {
        if (userId == null) {
            List<TenantEntity> allTenants = tenantRepository.findAll();
            return Result.success(tenantMapper.toDTO(allTenants));
        }

        List<UserTenantEntity> memberships = userTenantRepository.findByUserId(userId);
        List<TenantEntity> tenants = new ArrayList<>();

        for (UserTenantEntity membership : memberships) {
            if (membership.isActive()) {
                Optional<TenantEntity> tenantOpt = tenantRepository.findById(membership.getTenantId());
                tenantOpt.ifPresent(tenants::add);
            }
        }

        return Result.success(tenantMapper.toDTO(tenants));
    }
}
