package org.hermes.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.Permissions;
import org.hermes.infra.jooq.tables.RolePermissions;
import org.hermes.infra.jooq.tables.UserPermissions;
import org.hermes.infra.jooq.tables.records.PermissionsRecord;
import org.hermes.modules.authorization.domain.entity.PermissionEntity;
import org.hermes.modules.authorization.ports.out.PermissionRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PermissionRepositoryJooq extends GenericJooqRepository<PermissionEntity, PermissionsRecord, Long> implements PermissionRepository {

    private SnowflakeIdGenerator idGenerator;
    public PermissionRepositoryJooq() {
        super();
        this.idGenerator = null;
    }

    @Inject
    public PermissionRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                Permissions.PERMISSIONS,
                Permissions.PERMISSIONS.ID,
                Permissions.PERMISSIONS.VERSION,
                Permissions.PERMISSIONS.CREATED_AT,
                Permissions.PERMISSIONS.UPDATED_AT,
                Permissions.PERMISSIONS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected PermissionEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new PermissionEntity(
                r.get(Permissions.PERMISSIONS.ID),
                r.get(Permissions.PERMISSIONS.NAME),
                r.get(Permissions.PERMISSIONS.SLUG),
                r.get(Permissions.PERMISSIONS.MODULE),
                r.get(Permissions.PERMISSIONS.ACTION),
                r.get(Permissions.PERMISSIONS.DESCRIPTION),
                r.get(Permissions.PERMISSIONS.STATUS),
                r.get(Permissions.PERMISSIONS.IS_SYSTEM),
                r.get(Permissions.PERMISSIONS.VERSION),
                r.get(Permissions.PERMISSIONS.CREATED_AT),
                r.get(Permissions.PERMISSIONS.UPDATED_AT),
                r.get(Permissions.PERMISSIONS.DELETED_AT)
        );
    }

    @Override
    public PermissionEntity insert(PermissionEntity entity) {
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

        dsl.insertInto(Permissions.PERMISSIONS)
                .set(Permissions.PERMISSIONS.ID, entity.getId())
                .set(Permissions.PERMISSIONS.NAME, entity.getName())
                .set(Permissions.PERMISSIONS.SLUG, entity.getSlug())
                .set(Permissions.PERMISSIONS.MODULE, entity.getModule())
                .set(Permissions.PERMISSIONS.ACTION, entity.getAction())
                .set(Permissions.PERMISSIONS.DESCRIPTION, entity.getDescription())
                .set(Permissions.PERMISSIONS.STATUS, entity.getStatus())
                .set(Permissions.PERMISSIONS.IS_SYSTEM, entity.getIsSystem())
                .set(Permissions.PERMISSIONS.VERSION, entity.getVersion())
                .set(Permissions.PERMISSIONS.CREATED_AT, entity.getCreatedAt())
                .set(Permissions.PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(Permissions.PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public PermissionEntity save(PermissionEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(Permissions.PERMISSIONS)
                .set(Permissions.PERMISSIONS.NAME, entity.getName())
                .set(Permissions.PERMISSIONS.SLUG, entity.getSlug())
                .set(Permissions.PERMISSIONS.MODULE, entity.getModule())
                .set(Permissions.PERMISSIONS.ACTION, entity.getAction())
                .set(Permissions.PERMISSIONS.DESCRIPTION, entity.getDescription())
                .set(Permissions.PERMISSIONS.STATUS, entity.getStatus())
                .set(Permissions.PERMISSIONS.IS_SYSTEM, entity.getIsSystem())
                .set(Permissions.PERMISSIONS.VERSION, entity.getVersion())
                .set(Permissions.PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(Permissions.PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .where(Permissions.PERMISSIONS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<PermissionEntity> findBySlug(String slug) {
        if (slug == null) return Optional.empty();
        return dsl.selectFrom(Permissions.PERMISSIONS)
                .where(Permissions.PERMISSIONS.SLUG.equalIgnoreCase(slug)
                        .and(Permissions.PERMISSIONS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public boolean existsBySlug(String slug) {
        if (slug == null) return false;
        return dsl.fetchExists(
                Permissions.PERMISSIONS,
                Permissions.PERMISSIONS.SLUG.equalIgnoreCase(slug).and(Permissions.PERMISSIONS.DELETED_AT.isNull())
        );
    }

    @Override
    public List<PermissionEntity> findByModule(String module) {
        if (module == null) return List.of();
        return dsl.selectFrom(Permissions.PERMISSIONS)
                .where(Permissions.PERMISSIONS.MODULE.equalIgnoreCase(module)
                        .and(Permissions.PERMISSIONS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public List<PermissionEntity> findByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return List.of();
        return dsl.select(Permissions.PERMISSIONS.fields())
                .from(Permissions.PERMISSIONS)
                .join(RolePermissions.ROLE_PERMISSIONS)
                .on(Permissions.PERMISSIONS.ID.eq(RolePermissions.ROLE_PERMISSIONS.PERMISSION_ID))
                .where(RolePermissions.ROLE_PERMISSIONS.ROLE_ID.in(roleIds)
                        .and(RolePermissions.ROLE_PERMISSIONS.DELETED_AT.isNull())
                        .and(Permissions.PERMISSIONS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public List<PermissionEntity> findDirectPermissionsByUserTenantId(Long userTenantId) {
        if (userTenantId == null) return List.of();
        return dsl.select(Permissions.PERMISSIONS.fields())
                .from(Permissions.PERMISSIONS)
                .join(UserPermissions.USER_PERMISSIONS)
                .on(Permissions.PERMISSIONS.ID.eq(UserPermissions.USER_PERMISSIONS.PERMISSION_ID))
                .where(UserPermissions.USER_PERMISSIONS.USER_TENANT_ID.eq(userTenantId)
                        .and(UserPermissions.USER_PERMISSIONS.IS_ACTIVE.isTrue())
                        .and(UserPermissions.USER_PERMISSIONS.DELETED_AT.isNull())
                        .and(Permissions.PERMISSIONS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }
}
