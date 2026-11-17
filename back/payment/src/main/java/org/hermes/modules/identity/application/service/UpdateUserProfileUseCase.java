package org.hermes.modules.identity.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.identity.application.dto.UpdateUserDTO;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.ports.in.IUpdateUserProfileUseCase;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class UpdateUserProfileUseCase implements IUpdateUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Inject
    public UpdateUserProfileUseCase(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @ResultTransaction
    public Result<UserDTO> execute(Long userId, UpdateUserDTO dto) {
        if (userId == null || dto == null) {
            return Result.failure("Invalid user update data", 400);
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().isDeleted()) {
            return Result.notFound("User not found");
        }

        UserEntity user = userOpt.get();
        userMapper.updateEntityFromDto(dto, user);
        user = userRepository.save(user);

        return Result.success(userMapper.toDTO(user));
    }
}
