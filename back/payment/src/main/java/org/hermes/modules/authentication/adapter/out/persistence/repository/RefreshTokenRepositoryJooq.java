package org.hermes.modules.authentication.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.RefreshTokens;
import org.hermes.infra.jooq.tables.records.RefreshTokensRecord;
import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class RefreshTokenRepositoryJooq extends GenericJooqRepository<RefreshTokenEntity, RefreshTokensRecord, Long> implements RefreshTokenRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public RefreshTokenRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                RefreshTokens.REFRESH_TOKENS,
                RefreshTokens.REFRESH_TOKENS.ID,
                RefreshTokens.REFRESH_TOKENS.VERSION,
                RefreshTokens.REFRESH_TOKENS.CREATED_AT,
                RefreshTokens.REFRESH_TOKENS.UPDATED_AT,
                RefreshTokens.REFRESH_TOKENS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected RefreshTokenEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new RefreshTokenEntity(
                r.get(RefreshTokens.REFRESH_TOKENS.ID),
                r.get(RefreshTokens.REFRESH_TOKENS.SESSION_ID),
                r.get(RefreshTokens.REFRESH_TOKENS.USER_ID),
                r.get(RefreshTokens.REFRESH_TOKENS.TOKEN_HASH),
                r.get(RefreshTokens.REFRESH_TOKENS.IS_USED),
                r.get(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE),
                r.get(RefreshTokens.REFRESH_TOKENS.REPLACED_BY_TOKEN_ID),
                r.get(RefreshTokens.REFRESH_TOKENS.EXPIRED_AT),
                r.get(RefreshTokens.REFRESH_TOKENS.REVOKED_AT),
                r.get(RefreshTokens.REFRESH_TOKENS.VERSION),
                r.get(RefreshTokens.REFRESH_TOKENS.CREATED_AT),
                r.get(RefreshTokens.REFRESH_TOKENS.UPDATED_AT),
                r.get(RefreshTokens.REFRESH_TOKENS.DELETED_AT)
        );
    }

    @Override
    public RefreshTokenEntity insert(RefreshTokenEntity entity) {
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

        dsl.insertInto(RefreshTokens.REFRESH_TOKENS)
                .set(RefreshTokens.REFRESH_TOKENS.ID, entity.getId())
                .set(RefreshTokens.REFRESH_TOKENS.SESSION_ID, entity.getSessionId())
                .set(RefreshTokens.REFRESH_TOKENS.USER_ID, entity.getUserId())
                .set(RefreshTokens.REFRESH_TOKENS.TOKEN_HASH, entity.getTokenHash())
                .set(RefreshTokens.REFRESH_TOKENS.IS_USED, entity.getIsUsed())
                .set(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE, entity.getIsActive())
                .set(RefreshTokens.REFRESH_TOKENS.REPLACED_BY_TOKEN_ID, entity.getReplacedByTokenId())
                .set(RefreshTokens.REFRESH_TOKENS.EXPIRED_AT, entity.getExpiredAt())
                .set(RefreshTokens.REFRESH_TOKENS.REVOKED_AT, entity.getRevokedAt())
                .set(RefreshTokens.REFRESH_TOKENS.VERSION, entity.getVersion())
                .set(RefreshTokens.REFRESH_TOKENS.CREATED_AT, entity.getCreatedAt())
                .set(RefreshTokens.REFRESH_TOKENS.UPDATED_AT, entity.getUpdatedAt())
                .set(RefreshTokens.REFRESH_TOKENS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public RefreshTokenEntity save(RefreshTokenEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(RefreshTokens.REFRESH_TOKENS)
                .set(RefreshTokens.REFRESH_TOKENS.IS_USED, entity.getIsUsed())
                .set(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE, entity.getIsActive())
                .set(RefreshTokens.REFRESH_TOKENS.REPLACED_BY_TOKEN_ID, entity.getReplacedByTokenId())
                .set(RefreshTokens.REFRESH_TOKENS.EXPIRED_AT, entity.getExpiredAt())
                .set(RefreshTokens.REFRESH_TOKENS.REVOKED_AT, entity.getRevokedAt())
                .set(RefreshTokens.REFRESH_TOKENS.VERSION, entity.getVersion())
                .set(RefreshTokens.REFRESH_TOKENS.UPDATED_AT, entity.getUpdatedAt())
                .set(RefreshTokens.REFRESH_TOKENS.DELETED_AT, entity.getDeletedAt())
                .where(RefreshTokens.REFRESH_TOKENS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<RefreshTokenEntity> findByTokenHash(String tokenHash) {
        if (tokenHash == null) return Optional.empty();
        return dsl.selectFrom(RefreshTokens.REFRESH_TOKENS)
                .where(RefreshTokens.REFRESH_TOKENS.TOKEN_HASH.eq(tokenHash)
                        .and(RefreshTokens.REFRESH_TOKENS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public int revokeAllByUserId(Long userId) {
        if (userId == null) return 0;
        return dsl.update(RefreshTokens.REFRESH_TOKENS)
                .set(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE, false)
                .set(RefreshTokens.REFRESH_TOKENS.REVOKED_AT, OffsetDateTime.now())
                .where(RefreshTokens.REFRESH_TOKENS.USER_ID.eq(userId)
                        .and(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE.isTrue())
                        .and(RefreshTokens.REFRESH_TOKENS.DELETED_AT.isNull()))
                .execute();
    }

    @Override
    public int revokeBySessionId(Long sessionId) {
        if (sessionId == null) return 0;
        return dsl.update(RefreshTokens.REFRESH_TOKENS)
                .set(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE, false)
                .set(RefreshTokens.REFRESH_TOKENS.REVOKED_AT, OffsetDateTime.now())
                .where(RefreshTokens.REFRESH_TOKENS.SESSION_ID.eq(sessionId)
                        .and(RefreshTokens.REFRESH_TOKENS.IS_ACTIVE.isTrue())
                        .and(RefreshTokens.REFRESH_TOKENS.DELETED_AT.isNull()))
                .execute();
    }
}
