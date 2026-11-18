package org.hermes.modules.authorization.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authorization.application.dto.PermissionDTO;
import org.hermes.modules.authorization.application.mapper.PermissionMapper;
import org.hermes.modules.authorization.ports.in.IListPermissionsUseCase;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.shared.result.Result;

import java.util.List;

@ApplicationScoped
public class ListPermissionsUseCase implements IListPermissionsUseCase {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Inject
    public ListPermissionsUseCase(PermissionRepository permissionRepository, PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Result<List<PermissionDTO>> execute() {
        return Result.success(permissionMapper.toDTO(permissionRepository.findAll()));
    }
}
