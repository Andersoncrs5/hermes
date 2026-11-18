package org.hermes.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.RolePermissions;
import org.hermes.infra.jooq.tables.records.RolePermissionsRecord;
import org.hermes.modules.authorization.domain.entity.RolePermissionEntity;
import org.hermes.modules.authorization.ports.out.RolePermissionRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RolePermissionRepositoryJooq extends GenericJooqRepository<RolePermissionEntity, RolePermissionsRecord, Long> implements RolePermissionRepository {

    private SnowflakeIdGenerator idGenerator;
    public RolePermissionRepositoryJooq() {
        super();
        this.idGenerator = null;
    }

    @Inject
    public RolePermissionRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                RolePermissions.ROLE_PERMISSIONS,
                RolePermissions.ROLE_PERMISSIONS.ID,
                RolePermissions.ROLE_PERMISSIONS.VERSION,
                RolePermissions.ROLE_PERMISSIONS.CREATED_AT,
                RolePermissions.ROLE_PERMISSIONS.UPDATED_AT,
                RolePermissions.ROLE_PERMISSIONS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected RolePermissionEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new RolePermissionEntity(
                r.get(RolePermissions.ROLE_PERMISSIONS.ID),
                r.get(RolePermissions.ROLE_PERMISSIONS.ROLE_ID),
                r.get(RolePermissions.ROLE_PERMISSIONS.PERMISSION_ID),
                r.get(RolePermissions.ROLE_PERMISSIONS.VERSION),
                r.get(RolePermissions.ROLE_PERMISSIONS.CREATED_AT),
                r.get(RolePermissions.ROLE_PERMISSIONS.UPDATED_AT),
                r.get(RolePermissions.ROLE_PERMISSIONS.DELETED_AT)
        );
    }

    @Override
    public RolePermissionEntity insert(RolePermissionEntity entity) {
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

        dsl.insertInto(RolePermissions.ROLE_PERMISSIONS)
                .set(RolePermissions.ROLE_PERMISSIONS.ID, entity.getId())
                .set(RolePermissions.ROLE_PERMISSIONS.ROLE_ID, entity.getRoleId())
                .set(RolePermissions.ROLE_PERMISSIONS.PERMISSION_ID, entity.getPermissionId())
                .set(RolePermissions.ROLE_PERMISSIONS.VERSION, entity.getVersion())
                .set(RolePermissions.ROLE_PERMISSIONS.CREATED_AT, entity.getCreatedAt())
                .set(RolePermissions.ROLE_PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(RolePermissions.ROLE_PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public RolePermissionEntity save(RolePermissionEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(RolePermissions.ROLE_PERMISSIONS)
                .set(RolePermissions.ROLE_PERMISSIONS.ROLE_ID, entity.getRoleId())
                .set(RolePermissions.ROLE_PERMISSIONS.PERMISSION_ID, entity.getPermissionId())
                .set(RolePermissions.ROLE_PERMISSIONS.VERSION, entity.getVersion())
                .set(RolePermissions.ROLE_PERMISSIONS.UPDATED_AT, entity.getUpdatedAt())
                .set(RolePermissions.ROLE_PERMISSIONS.DELETED_AT, entity.getDeletedAt())
                .where(RolePermissions.ROLE_PERMISSIONS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public List<RolePermissionEntity> findByRoleId(Long roleId) {
        if (roleId == null) return List.of();
        return dsl.selectFrom(RolePermissions.ROLE_PERMISSIONS)
                .where(RolePermissions.ROLE_PERMISSIONS.ROLE_ID.eq(roleId)
                        .and(RolePermissions.ROLE_PERMISSIONS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public Optional<RolePermissionEntity> findByRoleAndPermission(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) return Optional.empty();
        return dsl.selectFrom(RolePermissions.ROLE_PERMISSIONS)
                .where(RolePermissions.ROLE_PERMISSIONS.ROLE_ID.eq(roleId)
                        .and(RolePermissions.ROLE_PERMISSIONS.PERMISSION_ID.eq(permissionId))
                        .and(RolePermissions.ROLE_PERMISSIONS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public int deleteByRoleAndPermission(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) return 0;
        return dsl.update(RolePermissions.ROLE_PERMISSIONS)
                .set(RolePermissions.ROLE_PERMISSIONS.DELETED_AT, OffsetDateTime.now())
                .where(RolePermissions.ROLE_PERMISSIONS.ROLE_ID.eq(roleId)
                        .and(RolePermissions.ROLE_PERMISSIONS.PERMISSION_ID.eq(permissionId))
                        .and(RolePermissions.ROLE_PERMISSIONS.DELETED_AT.isNull()))
                .execute();
    }
}
