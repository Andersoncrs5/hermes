package org.hermes.modules.authentication.ports.out;

import org.hermes.modules.authentication.domain.entity.LoginAttemptEntity;

import java.time.OffsetDateTime;
import java.util.List;

public interface LoginAttemptRepository {

    void save(LoginAttemptEntity attempt);

    long countRecentFailedAttemptsByIp(String ipAddress, OffsetDateTime since);

    long countRecentFailedAttemptsByUsername(String username, OffsetDateTime since);

    List<LoginAttemptEntity> findRecentByUsername(String username, int limit);
}
