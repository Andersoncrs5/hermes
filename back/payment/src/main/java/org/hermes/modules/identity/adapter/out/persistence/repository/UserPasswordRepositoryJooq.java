package org.hermes.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.UserPasswords;
import org.hermes.infra.jooq.tables.records.UserPasswordsRecord;
import org.hermes.modules.identity.domain.entity.UserPasswordEntity;
import org.hermes.modules.identity.ports.out.UserPasswordRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class UserPasswordRepositoryJooq extends GenericJooqRepository<UserPasswordEntity, UserPasswordsRecord, Long> implements UserPasswordRepository {

    private SnowflakeIdGenerator idGenerator;
    public UserPasswordRepositoryJooq() {
        super();
        this.idGenerator = null;
    }

    @Inject
    public UserPasswordRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                UserPasswords.USER_PASSWORDS,
                UserPasswords.USER_PASSWORDS.ID,
                UserPasswords.USER_PASSWORDS.VERSION,
                UserPasswords.USER_PASSWORDS.CREATED_AT,
                UserPasswords.USER_PASSWORDS.UPDATED_AT,
                UserPasswords.USER_PASSWORDS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected UserPasswordEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new UserPasswordEntity(
                r.get(UserPasswords.USER_PASSWORDS.ID),
                r.get(UserPasswords.USER_PASSWORDS.USER_ID),
                r.get(UserPasswords.USER_PASSWORDS.PASSWORD_HASH),
                r.get(UserPasswords.USER_PASSWORDS.TYPE_HASH),
                r.get(UserPasswords.USER_PASSWORDS.SHOULD_UPDATE),
                r.get(UserPasswords.USER_PASSWORDS.VERSION),
                r.get(UserPasswords.USER_PASSWORDS.CREATED_AT),
                r.get(UserPasswords.USER_PASSWORDS.UPDATED_AT),
                r.get(UserPasswords.USER_PASSWORDS.DELETED_AT)
        );
    }

    @Override
    public UserPasswordEntity insert(UserPasswordEntity entity) {
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

        dsl.insertInto(UserPasswords.USER_PASSWORDS)
                .set(UserPasswords.USER_PASSWORDS.ID, entity.getId())
                .set(UserPasswords.USER_PASSWORDS.USER_ID, entity.getUserId())
                .set(UserPasswords.USER_PASSWORDS.PASSWORD_HASH, entity.getPasswordHash())
                .set(UserPasswords.USER_PASSWORDS.TYPE_HASH, entity.getTypeHash())
                .set(UserPasswords.USER_PASSWORDS.SHOULD_UPDATE, entity.getShouldUpdate())
                .set(UserPasswords.USER_PASSWORDS.VERSION, entity.getVersion())
                .set(UserPasswords.USER_PASSWORDS.CREATED_AT, entity.getCreatedAt())
                .set(UserPasswords.USER_PASSWORDS.UPDATED_AT, entity.getUpdatedAt())
                .set(UserPasswords.USER_PASSWORDS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public UserPasswordEntity save(UserPasswordEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(UserPasswords.USER_PASSWORDS)
                .set(UserPasswords.USER_PASSWORDS.USER_ID, entity.getUserId())
                .set(UserPasswords.USER_PASSWORDS.PASSWORD_HASH, entity.getPasswordHash())
                .set(UserPasswords.USER_PASSWORDS.TYPE_HASH, entity.getTypeHash())
                .set(UserPasswords.USER_PASSWORDS.SHOULD_UPDATE, entity.getShouldUpdate())
                .set(UserPasswords.USER_PASSWORDS.VERSION, entity.getVersion())
                .set(UserPasswords.USER_PASSWORDS.UPDATED_AT, entity.getUpdatedAt())
                .set(UserPasswords.USER_PASSWORDS.DELETED_AT, entity.getDeletedAt())
                .where(UserPasswords.USER_PASSWORDS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<UserPasswordEntity> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return dsl.selectFrom(UserPasswords.USER_PASSWORDS)
                .where(UserPasswords.USER_PASSWORDS.USER_ID.eq(userId)
                        .and(UserPasswords.USER_PASSWORDS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public int deleteByUserId(Long userId) {
        if (userId == null) return 0;
        return dsl.update(UserPasswords.USER_PASSWORDS)
                .set(UserPasswords.USER_PASSWORDS.DELETED_AT, OffsetDateTime.now())
                .where(UserPasswords.USER_PASSWORDS.USER_ID.eq(userId)
                        .and(UserPasswords.USER_PASSWORDS.DELETED_AT.isNull()))
                .execute();
    }
}
