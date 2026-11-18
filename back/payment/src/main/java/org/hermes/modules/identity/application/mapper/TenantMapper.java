package org.hermes.modules.identity.application.mapper;

import org.hermes.modules.identity.application.dto.CreateTenantDTO;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.shared.mapper.CentralMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface TenantMapper {

    TenantDTO toDTO(TenantEntity entity);

    List<TenantDTO> toDTO(List<TenantEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "countryCode", defaultValue = "BR")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    TenantEntity toEntity(CreateTenantDTO dto);
}
