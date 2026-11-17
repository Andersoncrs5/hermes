package org.hermes.modules.authentication.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.LoginAttempts;
import org.hermes.modules.authentication.domain.entity.LoginAttemptEntity;
import org.hermes.modules.authentication.ports.out.LoginAttemptRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class LoginAttemptRepositoryJooq implements LoginAttemptRepository {

    private final DSLContext dsl;
    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public LoginAttemptRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
    }

    private LoginAttemptEntity mapRecord(Record r) {
        if (r == null) return null;
        return new LoginAttemptEntity(
                r.get(LoginAttempts.LOGIN_ATTEMPTS.ID),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.USER_ID),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.USERNAME),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.IP_ADDRESS),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.USER_AGENT),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.DEVICE_ID),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.ATTEMPT_AT),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.SUCCESS),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.FAILURE_REASON),
                r.get(LoginAttempts.LOGIN_ATTEMPTS.TRACE_ID)
        );
    }

    @Override
    public void save(LoginAttemptEntity attempt) {
        if (attempt.getId() == null) {
            attempt.setId(idGenerator.nextId());
        }
        if (attempt.getAttemptAt() == null) {
            attempt.setAttemptAt(OffsetDateTime.now());
        }

        dsl.insertInto(LoginAttempts.LOGIN_ATTEMPTS)
                .set(LoginAttempts.LOGIN_ATTEMPTS.ID, attempt.getId())
                .set(LoginAttempts.LOGIN_ATTEMPTS.USER_ID, attempt.getUserId())
                .set(LoginAttempts.LOGIN_ATTEMPTS.USERNAME, attempt.getUsername())
                .set(LoginAttempts.LOGIN_ATTEMPTS.IP_ADDRESS, attempt.getIpAddress())
                .set(LoginAttempts.LOGIN_ATTEMPTS.USER_AGENT, attempt.getUserAgent())
                .set(LoginAttempts.LOGIN_ATTEMPTS.DEVICE_ID, attempt.getDeviceId())
                .set(LoginAttempts.LOGIN_ATTEMPTS.ATTEMPT_AT, attempt.getAttemptAt())
                .set(LoginAttempts.LOGIN_ATTEMPTS.SUCCESS, attempt.getSuccess())
                .set(LoginAttempts.LOGIN_ATTEMPTS.FAILURE_REASON, attempt.getFailureReason())
                .set(LoginAttempts.LOGIN_ATTEMPTS.TRACE_ID, attempt.getTraceId())
                .execute();
    }

    @Override
    public long countRecentFailedAttemptsByIp(String ipAddress, OffsetDateTime since) {
        if (ipAddress == null) return 0L;
        Long count = dsl.selectCount()
                .from(LoginAttempts.LOGIN_ATTEMPTS)
                .where(LoginAttempts.LOGIN_ATTEMPTS.IP_ADDRESS.eq(ipAddress)
                        .and(LoginAttempts.LOGIN_ATTEMPTS.SUCCESS.isFalse())
                        .and(LoginAttempts.LOGIN_ATTEMPTS.ATTEMPT_AT.ge(since)))
                .fetchOne(0, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countRecentFailedAttemptsByUsername(String username, OffsetDateTime since) {
        if (username == null) return 0L;
        Long count = dsl.selectCount()
                .from(LoginAttempts.LOGIN_ATTEMPTS)
                .where(LoginAttempts.LOGIN_ATTEMPTS.USERNAME.equalIgnoreCase(username)
                        .and(LoginAttempts.LOGIN_ATTEMPTS.SUCCESS.isFalse())
                        .and(LoginAttempts.LOGIN_ATTEMPTS.ATTEMPT_AT.ge(since)))
                .fetchOne(0, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<LoginAttemptEntity> findRecentByUsername(String username, int limit) {
        if (username == null) return List.of();
        return dsl.selectFrom(LoginAttempts.LOGIN_ATTEMPTS)
                .where(LoginAttempts.LOGIN_ATTEMPTS.USERNAME.equalIgnoreCase(username))
                .orderBy(LoginAttempts.LOGIN_ATTEMPTS.ATTEMPT_AT.desc())
                .limit(Math.max(1, limit))
                .fetch(this::mapRecord);
    }
}
