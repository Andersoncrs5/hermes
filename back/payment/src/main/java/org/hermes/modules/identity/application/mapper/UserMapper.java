package org.hermes.modules.identity.application.mapper;

import org.hermes.modules.identity.application.dto.CreateUserDTO;
import org.hermes.modules.identity.application.dto.UpdateUserDTO;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.shared.mapper.CentralMapperConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {

    UserDTO toDTO(UserEntity entity);

    List<UserDTO> toDTO(List<UserEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isEmailVerified", constant = "false")
    @Mapping(target = "failedLoginAttempts", constant = "0")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "blockedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    UserEntity toEntity(CreateUserDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateUserDTO dto, @MappingTarget UserEntity entity);
}
