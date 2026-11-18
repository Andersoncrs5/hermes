package org.hermes.modules.authentication.ports.out;

import org.hermes.modules.authentication.domain.entity.SessionEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;

public interface SessionRepository extends GenericRepository<SessionEntity, Long> {

    List<SessionEntity> findActiveByUserId(Long userId);

    int revokeAllByUserId(Long userId, String reason);
}
