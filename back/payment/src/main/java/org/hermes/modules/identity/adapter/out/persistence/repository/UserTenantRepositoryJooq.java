package org.hermes.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.UserTenants;
import org.hermes.infra.jooq.tables.records.UserTenantsRecord;
import org.hermes.modules.identity.domain.entity.UserTenantEntity;
import org.hermes.modules.identity.ports.out.UserTenantRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserTenantRepositoryJooq extends GenericJooqRepository<UserTenantEntity, UserTenantsRecord, Long> implements UserTenantRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public UserTenantRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                UserTenants.USER_TENANTS,
                UserTenants.USER_TENANTS.ID,
                UserTenants.USER_TENANTS.VERSION,
                UserTenants.USER_TENANTS.CREATED_AT,
                UserTenants.USER_TENANTS.UPDATED_AT,
                UserTenants.USER_TENANTS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected UserTenantEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new UserTenantEntity(
                r.get(UserTenants.USER_TENANTS.ID),
                r.get(UserTenants.USER_TENANTS.USER_ID),
                r.get(UserTenants.USER_TENANTS.TENANT_ID),
                r.get(UserTenants.USER_TENANTS.STATUS),
                r.get(UserTenants.USER_TENANTS.JOINED_AT),
                r.get(UserTenants.USER_TENANTS.INVITED_BY),
                r.get(UserTenants.USER_TENANTS.VERSION),
                r.get(UserTenants.USER_TENANTS.CREATED_AT),
                r.get(UserTenants.USER_TENANTS.UPDATED_AT),
                r.get(UserTenants.USER_TENANTS.DELETED_AT)
        );
    }

    @Override
    public UserTenantEntity insert(UserTenantEntity entity) {
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

        dsl.insertInto(UserTenants.USER_TENANTS)
                .set(UserTenants.USER_TENANTS.ID, entity.getId())
                .set(UserTenants.USER_TENANTS.USER_ID, entity.getUserId())
                .set(UserTenants.USER_TENANTS.TENANT_ID, entity.getTenantId())
                .set(UserTenants.USER_TENANTS.STATUS, entity.getStatus())
                .set(UserTenants.USER_TENANTS.JOINED_AT, entity.getJoinedAt())
                .set(UserTenants.USER_TENANTS.INVITED_BY, entity.getInvitedBy())
                .set(UserTenants.USER_TENANTS.VERSION, entity.getVersion())
                .set(UserTenants.USER_TENANTS.CREATED_AT, entity.getCreatedAt())
                .set(UserTenants.USER_TENANTS.UPDATED_AT, entity.getUpdatedAt())
                .set(UserTenants.USER_TENANTS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public UserTenantEntity save(UserTenantEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(UserTenants.USER_TENANTS)
                .set(UserTenants.USER_TENANTS.STATUS, entity.getStatus())
                .set(UserTenants.USER_TENANTS.JOINED_AT, entity.getJoinedAt())
                .set(UserTenants.USER_TENANTS.INVITED_BY, entity.getInvitedBy())
                .set(UserTenants.USER_TENANTS.VERSION, entity.getVersion())
                .set(UserTenants.USER_TENANTS.UPDATED_AT, entity.getUpdatedAt())
                .set(UserTenants.USER_TENANTS.DELETED_AT, entity.getDeletedAt())
                .where(UserTenants.USER_TENANTS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<UserTenantEntity> findByUserAndTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) return Optional.empty();
        return dsl.selectFrom(UserTenants.USER_TENANTS)
                .where(UserTenants.USER_TENANTS.USER_ID.eq(userId)
                        .and(UserTenants.USER_TENANTS.TENANT_ID.eq(tenantId))
                        .and(UserTenants.USER_TENANTS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public List<UserTenantEntity> findByUserId(Long userId) {
        if (userId == null) return List.of();
        return dsl.selectFrom(UserTenants.USER_TENANTS)
                .where(UserTenants.USER_TENANTS.USER_ID.eq(userId)
                        .and(UserTenants.USER_TENANTS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public List<UserTenantEntity> findByTenantId(Long tenantId) {
        if (tenantId == null) return List.of();
        return dsl.selectFrom(UserTenants.USER_TENANTS)
                .where(UserTenants.USER_TENANTS.TENANT_ID.eq(tenantId)
                        .and(UserTenants.USER_TENANTS.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public boolean existsByUserAndTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) return false;
        return dsl.fetchExists(
                UserTenants.USER_TENANTS,
                UserTenants.USER_TENANTS.USER_ID.eq(userId)
                        .and(UserTenants.USER_TENANTS.TENANT_ID.eq(tenantId))
                        .and(UserTenants.USER_TENANTS.DELETED_AT.isNull())
        );
    }
}
