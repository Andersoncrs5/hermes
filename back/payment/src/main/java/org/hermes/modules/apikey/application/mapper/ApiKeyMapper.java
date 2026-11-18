package org.hermes.modules.apikey.application.mapper;

import org.hermes.modules.apikey.application.dto.ApiKeyDTO;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.shared.mapper.CentralMapperConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface ApiKeyMapper {

    ApiKeyDTO toDTO(ApiKeyEntity entity);

    List<ApiKeyDTO> toDTO(List<ApiKeyEntity> entities);
}
