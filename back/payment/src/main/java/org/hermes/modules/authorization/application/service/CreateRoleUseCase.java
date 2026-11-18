package org.hermes.modules.authorization.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.application.dto.CreateRoleDTO;
import org.hermes.modules.authorization.application.dto.RoleDTO;
import org.hermes.modules.authorization.application.mapper.RoleMapper;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.ports.in.ICreateRoleUseCase;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class CreateRoleUseCase implements ICreateRoleUseCase {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Inject
    public CreateRoleUseCase(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    @ResultTransaction
    public Result<RoleDTO> execute(CreateRoleDTO dto) {
        if (dto == null || dto.name() == null || dto.name().isBlank()) {
            return Result.failure("Role name is required", 400);
        }

        String normalizedSlug = dto.name().trim().toLowerCase().replaceAll("\\s+", "-");
        Optional<RoleEntity> existing = roleRepository.findBySlug(normalizedSlug);
        if (existing.isPresent()) {
            return Result.conflict("Role with slug '" + normalizedSlug + "' already exists");
        }

        RoleEntity entity = roleMapper.toEntity(dto);
        entity.setName(dto.name().trim());
        entity.setSlug(normalizedSlug);
        entity.setIsSystem(false);
        entity = roleRepository.insert(entity);

        return Result.success(roleMapper.toDTO(entity));
    }
}
