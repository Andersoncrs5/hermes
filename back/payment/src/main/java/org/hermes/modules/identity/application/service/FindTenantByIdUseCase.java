package org.hermes.modules.identity.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.modules.identity.application.mapper.TenantMapper;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.modules.identity.ports.in.IFindTenantByIdUseCase;
import org.hermes.modules.identity.ports.out.TenantRepository;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class FindTenantByIdUseCase implements IFindTenantByIdUseCase {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Inject
    public FindTenantByIdUseCase(TenantRepository tenantRepository, TenantMapper tenantMapper) {
        this.tenantRepository = tenantRepository;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public Result<TenantDTO> execute(Long tenantId) {
        if (tenantId == null) {
            return Result.failure("Tenant ID is required", 400);
        }

        Optional<TenantEntity> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty() || tenantOpt.get().isDeleted()) {
            return Result.notFound("Tenant not found");
        }

        return Result.success(tenantMapper.toDTO(tenantOpt.get()));
    }
}
