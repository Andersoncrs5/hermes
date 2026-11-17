package org.hermes.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.Tenants;
import org.hermes.infra.jooq.tables.records.TenantsRecord;
import org.hermes.modules.identity.domain.entity.TenantEntity;
import org.hermes.modules.identity.ports.out.TenantRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class TenantRepositoryJooq extends GenericJooqRepository<TenantEntity, TenantsRecord, Long> implements TenantRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public TenantRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                Tenants.TENANTS,
                Tenants.TENANTS.ID,
                Tenants.TENANTS.VERSION,
                Tenants.TENANTS.CREATED_AT,
                Tenants.TENANTS.UPDATED_AT,
                Tenants.TENANTS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected TenantEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new TenantEntity(
                r.get(Tenants.TENANTS.ID),
                r.get(Tenants.TENANTS.NAME),
                r.get(Tenants.TENANTS.SLUG),
                r.get(Tenants.TENANTS.STATUS),
                r.get(Tenants.TENANTS.COUNTRY_CODE),
                r.get(Tenants.TENANTS.VERSION),
                r.get(Tenants.TENANTS.CREATED_AT),
                r.get(Tenants.TENANTS.UPDATED_AT),
                r.get(Tenants.TENANTS.DELETED_AT)
        );
    }

    @Override
    public TenantEntity insert(TenantEntity entity) {
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

        dsl.insertInto(Tenants.TENANTS)
                .set(Tenants.TENANTS.ID, entity.getId())
                .set(Tenants.TENANTS.NAME, entity.getName())
                .set(Tenants.TENANTS.SLUG, entity.getSlug())
                .set(Tenants.TENANTS.STATUS, entity.getStatus())
                .set(Tenants.TENANTS.COUNTRY_CODE, entity.getCountryCode())
                .set(Tenants.TENANTS.VERSION, entity.getVersion())
                .set(Tenants.TENANTS.CREATED_AT, entity.getCreatedAt())
                .set(Tenants.TENANTS.UPDATED_AT, entity.getUpdatedAt())
                .set(Tenants.TENANTS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public TenantEntity save(TenantEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(Tenants.TENANTS)
                .set(Tenants.TENANTS.NAME, entity.getName())
                .set(Tenants.TENANTS.SLUG, entity.getSlug())
                .set(Tenants.TENANTS.STATUS, entity.getStatus())
                .set(Tenants.TENANTS.COUNTRY_CODE, entity.getCountryCode())
                .set(Tenants.TENANTS.VERSION, entity.getVersion())
                .set(Tenants.TENANTS.UPDATED_AT, entity.getUpdatedAt())
                .set(Tenants.TENANTS.DELETED_AT, entity.getDeletedAt())
                .where(Tenants.TENANTS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<TenantEntity> findBySlug(String slug) {
        if (slug == null) return Optional.empty();
        return dsl.selectFrom(Tenants.TENANTS)
                .where(Tenants.TENANTS.SLUG.equalIgnoreCase(slug)
                        .and(Tenants.TENANTS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public boolean existsBySlug(String slug) {
        if (slug == null) return false;
        return dsl.fetchExists(
                Tenants.TENANTS,
                Tenants.TENANTS.SLUG.equalIgnoreCase(slug).and(Tenants.TENANTS.DELETED_AT.isNull())
        );
    }
}
