package org.hermes.modules.authorization.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.Roles;
import org.hermes.infra.jooq.tables.records.RolesRecord;
import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.modules.authorization.ports.out.RoleRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoleRepositoryJooq extends GenericJooqRepository<RoleEntity, RolesRecord, Long> implements RoleRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public RoleRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                Roles.ROLES,
                Roles.ROLES.ID,
                Roles.ROLES.VERSION,
                Roles.ROLES.CREATED_AT,
                Roles.ROLES.UPDATED_AT,
                Roles.ROLES.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected RoleEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new RoleEntity(
                r.get(Roles.ROLES.ID),
                r.get(Roles.ROLES.NAME),
                r.get(Roles.ROLES.SLUG),
                r.get(Roles.ROLES.DESCRIPTION),
                r.get(Roles.ROLES.IS_ACTIVE),
                r.get(Roles.ROLES.IS_SYSTEM),
                r.get(Roles.ROLES.VERSION),
                r.get(Roles.ROLES.CREATED_AT),
                r.get(Roles.ROLES.UPDATED_AT),
                r.get(Roles.ROLES.DELETED_AT)
        );
    }

    @Override
    public RoleEntity insert(RoleEntity entity) {
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

        dsl.insertInto(Roles.ROLES)
                .set(Roles.ROLES.ID, entity.getId())
                .set(Roles.ROLES.NAME, entity.getName())
                .set(Roles.ROLES.SLUG, entity.getSlug())
                .set(Roles.ROLES.DESCRIPTION, entity.getDescription())
                .set(Roles.ROLES.IS_ACTIVE, entity.getIsActive())
                .set(Roles.ROLES.IS_SYSTEM, entity.getIsSystem())
                .set(Roles.ROLES.VERSION, entity.getVersion())
                .set(Roles.ROLES.CREATED_AT, entity.getCreatedAt())
                .set(Roles.ROLES.UPDATED_AT, entity.getUpdatedAt())
                .set(Roles.ROLES.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public RoleEntity save(RoleEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(Roles.ROLES)
                .set(Roles.ROLES.NAME, entity.getName())
                .set(Roles.ROLES.SLUG, entity.getSlug())
                .set(Roles.ROLES.DESCRIPTION, entity.getDescription())
                .set(Roles.ROLES.IS_ACTIVE, entity.getIsActive())
                .set(Roles.ROLES.IS_SYSTEM, entity.getIsSystem())
                .set(Roles.ROLES.VERSION, entity.getVersion())
                .set(Roles.ROLES.UPDATED_AT, entity.getUpdatedAt())
                .set(Roles.ROLES.DELETED_AT, entity.getDeletedAt())
                .where(Roles.ROLES.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<RoleEntity> findBySlug(String slug) {
        if (slug == null) return Optional.empty();
        return dsl.selectFrom(Roles.ROLES)
                .where(Roles.ROLES.SLUG.equalIgnoreCase(slug)
                        .and(Roles.ROLES.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public boolean existsBySlug(String slug) {
        if (slug == null) return false;
        return dsl.fetchExists(
                Roles.ROLES,
                Roles.ROLES.SLUG.equalIgnoreCase(slug).and(Roles.ROLES.DELETED_AT.isNull())
        );
    }

    @Override
    public List<RoleEntity> findBySlugs(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return List.of();
        return dsl.selectFrom(Roles.ROLES)
                .where(Roles.ROLES.SLUG.in(slugs)
                        .and(Roles.ROLES.DELETED_AT.isNull()))
                .fetch(this::mapRecordToEntity);
    }
}
