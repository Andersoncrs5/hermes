package org.hermes.modules.identity.ports.out;

import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.Optional;

public interface UserRepository extends GenericRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailOrUsername(String identifier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
