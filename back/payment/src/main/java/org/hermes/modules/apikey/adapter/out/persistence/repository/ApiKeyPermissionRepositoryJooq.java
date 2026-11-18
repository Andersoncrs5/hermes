package org.hermes.modules.apikey.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.ApiKeyPermissions;
import org.hermes.infra.jooq.tables.records.ApiKeyPermissionsRecord;
import org.hermes.modules.apikey.domain.entity.ApiKeyPermissionEntity;
import org.hermes.modules.apikey.ports.out.ApiKeyPermissionRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class ApiKeyPermissionRepositoryJooq extends GenericJooqRepository<ApiKeyPermissionEntity, ApiKeyPermissionsRecord, Long> implements ApiKeyPermissionRepository {

    private SnowflakeIdGenerator idGenerator;
    public ApiKeyPermissionRepositoryJooq() {
        super();
        this.idGenerator = null;
    }

    @Inject
    public ApiKeyPermissionRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                ApiKeyPermissions.API_KEY_PERMISSIONS,
                ApiKeyPermissions.API_KEY_PERMISSIONS.ID,
                ApiKeyPermissions.API_KEY_PERMISSIONS.VERSION,
                ApiKeyPermissions.API_KEY_PERMISSIONS.CREATED_AT,
                ApiKeyPermissions.API_KEY_PERMISSIONS.UPDATED_AT,
                ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected ApiKeyPermissionEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new ApiKeyPermissionEntity(
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.ID),
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.API_KEY_ID),
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.PERMISSION_ID),
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.VERSION),
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.CREATED_AT),
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.UPDATED_AT),
                r.get(ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT)
        );
    }

    @Override
    public ApiKeyPermissionEntity insert(ApiKeyPermissionEntity entity) {
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

        dsl.insertInto(ApiKeyPermissions.API_KEY_PERMISSIONS)
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.ID, entity.getId())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.API_KEY_ID, entity.getApiKeyId())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.PERMISSION_ID, entity.getPermissionId())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.VERSION, entity.getVersion())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.CREATED_AT, entity.getCreatedAt())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public ApiKeyPermissionEntity save(ApiKeyPermissionEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(ApiKeyPermissions.API_KEY_PERMISSIONS)
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.API_KEY_ID, entity.getApiKeyId())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.PERMISSION_ID, entity.getPermissionId())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.VERSION, entity.getVersion())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .where(ApiKeyPermissions.API_KEY_PERMISSIONS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public List<ApiKeyPermissionEntity> findByApiKeyId(Long apiKeyId) {
        if (apiKeyId == null) return List.of();
        return dsl.selectFrom(ApiKeyPermissions.API_KEY_PERMISSIONS)
                .where(ApiKeyPermissions.API_KEY_PERMISSIONS.API_KEY_ID.eq(apiKeyId)
                        .and(ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public int deleteByApiKeyId(Long apiKeyId) {
        if (apiKeyId == null) return 0;
        return dsl.update(ApiKeyPermissions.API_KEY_PERMISSIONS)
                .set(ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT, OffsetDateTime.now())
                .where(ApiKeyPermissions.API_KEY_PERMISSIONS.API_KEY_ID.eq(apiKeyId)
                        .and(ApiKeyPermissions.API_KEY_PERMISSIONS.DELETED_AT.isNull()))
                .execute();
    }
}
