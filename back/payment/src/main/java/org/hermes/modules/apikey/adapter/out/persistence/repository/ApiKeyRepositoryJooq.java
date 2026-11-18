package org.hermes.modules.apikey.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.ApiKeys;
import org.hermes.infra.jooq.tables.records.ApiKeysRecord;
import org.hermes.modules.apikey.domain.entity.ApiKeyEntity;
import org.hermes.modules.apikey.ports.out.ApiKeyRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ApiKeyRepositoryJooq extends GenericJooqRepository<ApiKeyEntity, ApiKeysRecord, Long> implements ApiKeyRepository {

    private SnowflakeIdGenerator idGenerator;
    public ApiKeyRepositoryJooq() {
        super();
        this.idGenerator = null;
    }

    @Inject
    public ApiKeyRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                ApiKeys.API_KEYS,
                ApiKeys.API_KEYS.ID,
                ApiKeys.API_KEYS.VERSION,
                ApiKeys.API_KEYS.CREATED_AT,
                ApiKeys.API_KEYS.UPDATED_AT,
                ApiKeys.API_KEYS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected ApiKeyEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new ApiKeyEntity(
                r.get(ApiKeys.API_KEYS.ID),
                r.get(ApiKeys.API_KEYS.TENANT_ID),
                r.get(ApiKeys.API_KEYS.KEY_PREFIX),
                r.get(ApiKeys.API_KEYS.SECRET_HASH),
                r.get(ApiKeys.API_KEYS.NAME),
                r.get(ApiKeys.API_KEYS.STATUS),
                r.get(ApiKeys.API_KEYS.ENVIRONMENT),
                r.get(ApiKeys.API_KEYS.EXPIRED_AT),
                r.get(ApiKeys.API_KEYS.CREATED_BY),
                r.get(ApiKeys.API_KEYS.LAST_USED_AT),
                r.get(ApiKeys.API_KEYS.REVOKED_AT),
                r.get(ApiKeys.API_KEYS.VERSION),
                r.get(ApiKeys.API_KEYS.CREATED_AT),
                r.get(ApiKeys.API_KEYS.UPDATED_AT),
                r.get(ApiKeys.API_KEYS.DELETED_AT)
        );
    }

    @Override
    public ApiKeyEntity insert(ApiKeyEntity entity) {
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

        dsl.insertInto(ApiKeys.API_KEYS)
                .set(ApiKeys.API_KEYS.ID, entity.getId())
                .set(ApiKeys.API_KEYS.TENANT_ID, entity.getTenantId())
                .set(ApiKeys.API_KEYS.KEY_PREFIX, entity.getKeyPrefix())
                .set(ApiKeys.API_KEYS.SECRET_HASH, entity.getSecretHash())
                .set(ApiKeys.API_KEYS.NAME, entity.getName())
                .set(ApiKeys.API_KEYS.STATUS, entity.getStatus())
                .set(ApiKeys.API_KEYS.ENVIRONMENT, entity.getEnvironment())
                .set(ApiKeys.API_KEYS.EXPIRED_AT, entity.getExpiredAt())
                .set(ApiKeys.API_KEYS.CREATED_BY, entity.getCreatedBy())
                .set(ApiKeys.API_KEYS.LAST_USED_AT, entity.getLastUsedAt())
                .set(ApiKeys.API_KEYS.REVOKED_AT, entity.getRevokedAt())
                .set(ApiKeys.API_KEYS.VERSION, entity.getVersion())
                .set(ApiKeys.API_KEYS.CREATED_AT, entity.getCreatedAt())
                .set(ApiKeys.API_KEYS.UPDATED_AT, entity.getUpdatedAt())
                .set(ApiKeys.API_KEYS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public ApiKeyEntity save(ApiKeyEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(ApiKeys.API_KEYS)
                .set(ApiKeys.API_KEYS.NAME, entity.getName())
                .set(ApiKeys.API_KEYS.STATUS, entity.getStatus())
                .set(ApiKeys.API_KEYS.ENVIRONMENT, entity.getEnvironment())
                .set(ApiKeys.API_KEYS.EXPIRED_AT, entity.getExpiredAt())
                .set(ApiKeys.API_KEYS.LAST_USED_AT, entity.getLastUsedAt())
                .set(ApiKeys.API_KEYS.REVOKED_AT, entity.getRevokedAt())
                .set(ApiKeys.API_KEYS.VERSION, entity.getVersion())
                .set(ApiKeys.API_KEYS.UPDATED_AT, entity.getUpdatedAt())
                .set(ApiKeys.API_KEYS.DELETED_AT, entity.getDeletedAt())
                .where(ApiKeys.API_KEYS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<ApiKeyEntity> findByPrefix(String keyPrefix) {
        if (keyPrefix == null) return Optional.empty();
        return dsl.selectFrom(ApiKeys.API_KEYS)
                .where(ApiKeys.API_KEYS.KEY_PREFIX.eq(keyPrefix)
                        .and(ApiKeys.API_KEYS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public List<ApiKeyEntity> findByTenantId(Long tenantId) {
        if (tenantId == null) return List.of();
        return dsl.selectFrom(ApiKeys.API_KEYS)
                .where(ApiKeys.API_KEYS.TENANT_ID.eq(tenantId)
                        .and(ApiKeys.API_KEYS.DELETED_AT.isNull()))
                .orderBy(ApiKeys.API_KEYS.CREATED_AT.desc())
                .fetch(this::mapRecordToEntity);
    }
}
