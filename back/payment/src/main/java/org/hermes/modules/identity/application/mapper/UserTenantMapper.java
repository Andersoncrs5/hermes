package org.hermes.modules.identity.application.mapper;

import org.hermes.modules.identity.application.dto.UserTenantDTO;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.shared.mapper.CentralMapperConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface UserTenantMapper {

    UserTenantDTO toDTO(UserTenantEntity entity);

    List<UserTenantDTO> toDTO(List<UserTenantEntity> entities);
}
