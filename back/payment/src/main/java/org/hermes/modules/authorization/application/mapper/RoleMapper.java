package org.hermes.modules.authorization.application.mapper;

import org.hermes.modules.authorization.application.dto.CreateRoleDTO;
import org.hermes.modules.authorization.application.dto.RoleDTO;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.shared.mapper.CentralMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface RoleMapper {

    RoleDTO toDTO(RoleEntity entity);

    List<RoleDTO> toDTO(List<RoleEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isSystem", constant = "false")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    RoleEntity toEntity(CreateRoleDTO dto);
}
