package org.hermes.modules.identity.ports.out;

import org.hermes.modules.identity.domain.entity.UserPasswordEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.Optional;

public interface UserPasswordRepository extends GenericRepository<UserPasswordEntity, Long> {

    Optional<UserPasswordEntity> findByUserId(Long userId);

    int deleteByUserId(Long userId);
}
