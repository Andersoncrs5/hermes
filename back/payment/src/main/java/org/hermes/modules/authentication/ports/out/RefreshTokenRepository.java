package org.hermes.modules.authentication.ports.out;

import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends GenericRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    int revokeAllByUserId(Long userId);

    int revokeBySessionId(Long sessionId);
}
