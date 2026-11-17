package org.hermes.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.UserTenantRoles;
import org.hermes.infra.jooq.tables.records.UserTenantRolesRecord;
import org.hermes.modules.authorization.domain.entity.UserTenantRoleEntity;
import org.hermes.modules.authorization.ports.out.UserTenantRoleRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserTenantRoleRepositoryJooq extends GenericJooqRepository<UserTenantRoleEntity, UserTenantRolesRecord, Long> implements UserTenantRoleRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public UserTenantRoleRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                UserTenantRoles.USER_TENANT_ROLES,
                UserTenantRoles.USER_TENANT_ROLES.ID,
                UserTenantRoles.USER_TENANT_ROLES.VERSION,
                UserTenantRoles.USER_TENANT_ROLES.CREATED_AT,
                UserTenantRoles.USER_TENANT_ROLES.UPDATED_AT,
                UserTenantRoles.USER_TENANT_ROLES.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected UserTenantRoleEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new UserTenantRoleEntity(
                r.get(UserTenantRoles.USER_TENANT_ROLES.ID),
                r.get(UserTenantRoles.USER_TENANT_ROLES.USER_TENANT_ID),
                r.get(UserTenantRoles.USER_TENANT_ROLES.ROLE_ID),
                r.get(UserTenantRoles.USER_TENANT_ROLES.IS_ACTIVE),
                r.get(UserTenantRoles.USER_TENANT_ROLES.VERSION),
                r.get(UserTenantRoles.USER_TENANT_ROLES.CREATED_AT),
                r.get(UserTenantRoles.USER_TENANT_ROLES.UPDATED_AT),
                r.get(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT)
        );
    }

    @Override
    public UserTenantRoleEntity insert(UserTenantRoleEntity entity) {
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

        dsl.insertInto(UserTenantRoles.USER_TENANT_ROLES)
                .set(UserTenantRoles.USER_TENANT_ROLES.ID, entity.getId())
                .set(UserTenantRoles.USER_TENANT_ROLES.USER_TENANT_ID, entity.getUserTenantId())
                .set(UserTenantRoles.USER_TENANT_ROLES.ROLE_ID, entity.getRoleId())
                .set(UserTenantRoles.USER_TENANT_ROLES.IS_ACTIVE, entity.getIsActive())
                .set(UserTenantRoles.USER_TENANT_ROLES.VERSION, entity.getVersion())
                .set(UserTenantRoles.USER_TENANT_ROLES.CREATED_AT, entity.getCreatedAt())
                .set(UserTenantRoles.USER_TENANT_ROLES.UPDATED_AT, entity.getUpdatedAt())
                .set(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public UserTenantRoleEntity save(UserTenantRoleEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(UserTenantRoles.USER_TENANT_ROLES)
                .set(UserTenantRoles.USER_TENANT_ROLES.IS_ACTIVE, entity.getIsActive())
                .set(UserTenantRoles.USER_TENANT_ROLES.VERSION, entity.getVersion())
                .set(UserTenantRoles.USER_TENANT_ROLES.UPDATED_AT, entity.getUpdatedAt())
                .set(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT, entity.getDeletedAt())
                .where(UserTenantRoles.USER_TENANT_ROLES.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public List<UserTenantRoleEntity> findByUserTenantId(Long userTenantId) {
        if (userTenantId == null) return List.of();
        return dsl.selectFrom(UserTenantRoles.USER_TENANT_ROLES)
                .where(UserTenantRoles.USER_TENANT_ROLES.USER_TENANT_ID.eq(userTenantId)
                        .and(UserTenantRoles.USER_TENANT_ROLES.IS_ACTIVE.isTrue())
                        .and(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public Optional<UserTenantRoleEntity> findByUserTenantAndRole(Long userTenantId, Long roleId) {
        if (userTenantId == null || roleId == null) return Optional.empty();
        return dsl.selectFrom(UserTenantRoles.USER_TENANT_ROLES)
                .where(UserTenantRoles.USER_TENANT_ROLES.USER_TENANT_ID.eq(userTenantId)
                        .and(UserTenantRoles.USER_TENANT_ROLES.ROLE_ID.eq(roleId))
                        .and(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public int deleteByUserTenantAndRole(Long userTenantId, Long roleId) {
        if (userTenantId == null || roleId == null) return 0;
        return dsl.update(UserTenantRoles.USER_TENANT_ROLES)
                .set(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT, OffsetDateTime.now())
                .set(UserTenantRoles.USER_TENANT_ROLES.IS_ACTIVE, false)
                .where(UserTenantRoles.USER_TENANT_ROLES.USER_TENANT_ID.eq(userTenantId)
                        .and(UserTenantRoles.USER_TENANT_ROLES.ROLE_ID.eq(roleId))
                        .and(UserTenantRoles.USER_TENANT_ROLES.DELETED_AT.isNull()))
                .execute();
    }
}
