package org.hermes.modules.identity.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.modules.identity.application.mapper.UserMapper;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.ports.in.IFindUserByIdUseCase;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class FindUserByIdUseCase implements IFindUserByIdUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Inject
    public FindUserByIdUseCase(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Result<UserDTO> execute(Long userId) {
        if (userId == null) {
            return Result.failure("User ID is required", 400);
        }

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().isDeleted()) {
            return Result.notFound("User not found");
        }

        return Result.success(userMapper.toDTO(userOpt.get()));
    }
}
