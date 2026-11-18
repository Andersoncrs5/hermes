package org.hermes.modules.authentication.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.Sessions;
import org.hermes.infra.jooq.tables.records.SessionsRecord;
import org.hermes.modules.authentication.domain.entity.SessionEntity;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class SessionRepositoryJooq extends GenericJooqRepository<SessionEntity, SessionsRecord, Long> implements SessionRepository {

    private SnowflakeIdGenerator idGenerator;
    public SessionRepositoryJooq() {
        super();
        this.idGenerator = null;
    }

    @Inject
    public SessionRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                Sessions.SESSIONS,
                Sessions.SESSIONS.ID,
                Sessions.SESSIONS.VERSION,
                Sessions.SESSIONS.CREATED_AT,
                Sessions.SESSIONS.UPDATED_AT,
                Sessions.SESSIONS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected SessionEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new SessionEntity(
                r.get(Sessions.SESSIONS.ID),
                r.get(Sessions.SESSIONS.USER_ID),
                r.get(Sessions.SESSIONS.DEVICE_ID),
                r.get(Sessions.SESSIONS.IP_ADDRESS),
                r.get(Sessions.SESSIONS.USER_AGENT),
                r.get(Sessions.SESSIONS.LAST_ACTIVITY_AT),
                r.get(Sessions.SESSIONS.EXPIRES_AT),
                r.get(Sessions.SESSIONS.REVOKED_AT),
                r.get(Sessions.SESSIONS.REVOCATION_REASON),
                r.get(Sessions.SESSIONS.IS_ACTIVE),
                r.get(Sessions.SESSIONS.VERSION),
                r.get(Sessions.SESSIONS.CREATED_AT),
                r.get(Sessions.SESSIONS.UPDATED_AT),
                r.get(Sessions.SESSIONS.DELETED_AT)
        );
    }

    @Override
    public SessionEntity insert(SessionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.nextId());
        }
        if (entity.getVersion() == null) {
            entity.setVersion(0L);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }
        entity.setUpdatedAt(OffsetDateTime.now());

        dsl.insertInto(Sessions.SESSIONS)
                .set(Sessions.SESSIONS.ID, entity.getId())
                .set(Sessions.SESSIONS.USER_ID, entity.getUserId())
                .set(Sessions.SESSIONS.DEVICE_ID, entity.getDeviceId())
                .set(Sessions.SESSIONS.IP_ADDRESS, entity.getIpAddress())
                .set(Sessions.SESSIONS.USER_AGENT, entity.getUserAgent())
                .set(Sessions.SESSIONS.LAST_ACTIVITY_AT, entity.getLastActivityAt())
                .set(Sessions.SESSIONS.EXPIRES_AT, entity.getExpiresAt())
                .set(Sessions.SESSIONS.REVOKED_AT, entity.getRevokedAt())
                .set(Sessions.SESSIONS.REVOCATION_REASON, entity.getRevocationReason())
                .set(Sessions.SESSIONS.IS_ACTIVE, entity.getIsActive())
                .set(Sessions.SESSIONS.VERSION, entity.getVersion())
                .set(Sessions.SESSIONS.CREATED_AT, entity.getCreatedAt())
                .set(Sessions.SESSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(Sessions.SESSIONS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public SessionEntity save(SessionEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(Sessions.SESSIONS)
                .set(Sessions.SESSIONS.LAST_ACTIVITY_AT, entity.getLastActivityAt())
                .set(Sessions.SESSIONS.EXPIRES_AT, entity.getExpiresAt())
                .set(Sessions.SESSIONS.REVOKED_AT, entity.getRevokedAt())
                .set(Sessions.SESSIONS.REVOCATION_REASON, entity.getRevocationReason())
                .set(Sessions.SESSIONS.IS_ACTIVE, entity.getIsActive())
                .set(Sessions.SESSIONS.VERSION, entity.getVersion())
                .set(Sessions.SESSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(Sessions.SESSIONS.DELETED_AT, entity.getDeletedAt())
                .where(Sessions.SESSIONS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public List<SessionEntity> findActiveByUserId(Long userId) {
        if (userId == null) return List.of();
        OffsetDateTime now = OffsetDateTime.now();
        return dsl.selectFrom(Sessions.SESSIONS)
                .where(Sessions.SESSIONS.USER_ID.eq(userId)
                        .and(Sessions.SESSIONS.IS_ACTIVE.isTrue())
                        .and(Sessions.SESSIONS.REVOKED_AT.isNull())
                        .and(Sessions.SESSIONS.EXPIRES_AT.gt(now))
                        .and(Sessions.SESSIONS.DELETED_AT.isNull()))
                .orderBy(Sessions.SESSIONS.LAST_ACTIVITY_AT.desc())
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public int revokeAllByUserId(Long userId, String reason) {
        if (userId == null) return 0;
        return dsl.update(Sessions.SESSIONS)
                .set(Sessions.SESSIONS.IS_ACTIVE, false)
                .set(Sessions.SESSIONS.REVOKED_AT, OffsetDateTime.now())
                .set(Sessions.SESSIONS.REVOCATION_REASON, reason)
                .where(Sessions.SESSIONS.USER_ID.eq(userId)
                        .and(Sessions.SESSIONS.IS_ACTIVE.isTrue())
                        .and(Sessions.SESSIONS.DELETED_AT.isNull()))
                .execute();
    }
}
