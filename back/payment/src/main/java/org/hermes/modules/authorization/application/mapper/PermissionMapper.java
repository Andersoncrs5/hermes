package org.hermes.modules.authorization.application.mapper;

import org.hermes.modules.authorization.application.dto.PermissionDTO;
import org.hermes.modules.authorization.domain.entity.PermissionEntity;
import org.hermes.shared.mapper.CentralMapperConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface PermissionMapper {

    PermissionDTO toDTO(PermissionEntity entity);

    List<PermissionDTO> toDTO(List<PermissionEntity> entities);
}
