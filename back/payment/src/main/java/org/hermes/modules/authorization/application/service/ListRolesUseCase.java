package org.hermes.modules.authorization.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.application.dto.RoleDTO;
import org.hermes.modules.authorization.application.mapper.RoleMapper;
import org.hermes.modules.authorization.ports.in.IListRolesUseCase;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.shared.result.Result;

import java.util.List;

@ApplicationScoped
public class ListRolesUseCase implements IListRolesUseCase {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Inject
    public ListRolesUseCase(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public Result<List<RoleDTO>> execute() {
        return Result.success(roleMapper.toDTO(roleRepository.findAll()));
    }
}
