package org.hermes.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.UserPermissions;
import org.hermes.infra.jooq.tables.records.UserPermissionsRecord;
import org.hermes.modules.authorization.domain.entity.UserPermissionEntity;
import org.hermes.modules.authorization.ports.out.UserPermissionRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserPermissionRepositoryJooq extends GenericJooqRepository<UserPermissionEntity, UserPermissionsRecord, Long> implements UserPermissionRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public UserPermissionRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                UserPermissions.USER_PERMISSIONS,
                UserPermissions.USER_PERMISSIONS.ID,
                UserPermissions.USER_PERMISSIONS.VERSION,
                UserPermissions.USER_PERMISSIONS.CREATED_AT,
                UserPermissions.USER_PERMISSIONS.UPDATED_AT,
                UserPermissions.USER_PERMISSIONS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected UserPermissionEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new UserPermissionEntity(
                r.get(UserPermissions.USER_PERMISSIONS.ID),
                r.get(UserPermissions.USER_PERMISSIONS.USER_TENANT_ID),
                r.get(UserPermissions.USER_PERMISSIONS.PERMISSION_ID),
                r.get(UserPermissions.USER_PERMISSIONS.IS_ACTIVE),
                r.get(UserPermissions.USER_PERMISSIONS.VERSION),
                r.get(UserPermissions.USER_PERMISSIONS.CREATED_AT),
                r.get(UserPermissions.USER_PERMISSIONS.UPDATED_AT),
                r.get(UserPermissions.USER_PERMISSIONS.DELETED_AT)
        );
    }

    @Override
    public UserPermissionEntity insert(UserPermissionEntity entity) {
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

        dsl.insertInto(UserPermissions.USER_PERMISSIONS)
                .set(UserPermissions.USER_PERMISSIONS.ID, entity.getId())
                .set(UserPermissions.USER_PERMISSIONS.USER_TENANT_ID, entity.getUserTenantId())
                .set(UserPermissions.USER_PERMISSIONS.PERMISSION_ID, entity.getPermissionId())
                .set(UserPermissions.USER_PERMISSIONS.IS_ACTIVE, entity.getIsActive())
                .set(UserPermissions.USER_PERMISSIONS.VERSION, entity.getVersion())
                .set(UserPermissions.USER_PERMISSIONS.CREATED_AT, entity.getCreatedAt())
                .set(UserPermissions.USER_PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(UserPermissions.USER_PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public UserPermissionEntity save(UserPermissionEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(UserPermissions.USER_PERMISSIONS)
                .set(UserPermissions.USER_PERMISSIONS.IS_ACTIVE, entity.getIsActive())
                .set(UserPermissions.USER_PERMISSIONS.VERSION, entity.getVersion())
                .set(UserPermissions.USER_PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(UserPermissions.USER_PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .where(UserPermissions.USER_PERMISSIONS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public List<UserPermissionEntity> findByUserTenantId(Long userTenantId) {
        if (userTenantId == null) return List.of();
        return dsl.selectFrom(UserPermissions.USER_PERMISSIONS)
                .where(UserPermissions.USER_PERMISSIONS.USER_TENANT_ID.eq(userTenantId)
                        .and(UserPermissions.USER_PERMISSIONS.IS_ACTIVE.isTrue())
                        .and(UserPermissions.USER_PERMISSIONS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public Optional<UserPermissionEntity> findByUserTenantAndPermission(Long userTenantId, Long permissionId) {
        if (userTenantId == null || permissionId == null) return Optional.empty();
        return dsl.selectFrom(UserPermissions.USER_PERMISSIONS)
                .where(UserPermissions.USER_PERMISSIONS.USER_TENANT_ID.eq(userTenantId)
                        .and(UserPermissions.USER_PERMISSIONS.PERMISSION_ID.eq(permissionId))
                        .and(UserPermissions.USER_PERMISSIONS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public int deleteByUserTenantAndPermission(Long userTenantId, Long permissionId) {
        if (userTenantId == null || permissionId == null) return 0;
        return dsl.update(UserPermissions.USER_PERMISSIONS)
                .set(UserPermissions.USER_PERMISSIONS.DELETED_AT, OffsetDateTime.now())
                .set(UserPermissions.USER_PERMISSIONS.IS_ACTIVE, false)
                .where(UserPermissions.USER_PERMISSIONS.USER_TENANT_ID.eq(userTenantId)
                        .and(UserPermissions.USER_PERMISSIONS.PERMISSION_ID.eq(permissionId))
                        .and(UserPermissions.USER_PERMISSIONS.DELETED_AT.isNull()))
                .execute();
    }
}
