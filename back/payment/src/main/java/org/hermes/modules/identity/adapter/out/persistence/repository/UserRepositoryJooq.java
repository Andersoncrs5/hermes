package org.hermes.modules.identity.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.Users;
import org.hermes.infra.jooq.tables.records.UsersRecord;
import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.modules.identity.ports.out.UserRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.hermes.shared.infrastructure.persistence.jooq.GenericJooqRepository;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class UserRepositoryJooq extends GenericJooqRepository<UserEntity, UsersRecord, Long> implements UserRepository {

    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public UserRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        super(
                dsl,
                Users.USERS,
                Users.USERS.ID,
                Users.USERS.VERSION,
                Users.USERS.CREATED_AT,
                Users.USERS.UPDATED_AT,
                Users.USERS.DELETED_AT
        );
        this.idGenerator = idGenerator;
    }

    @Override
    protected UserEntity mapRecordToEntity(Record r) {
        if (r == null) return null;
        return new UserEntity(
                r.get(Users.USERS.ID),
                r.get(Users.USERS.EMAIL),
                r.get(Users.USERS.USERNAME),
                r.get(Users.USERS.FULL_NAME),
                r.get(Users.USERS.IS_ACTIVE),
                r.get(Users.USERS.IS_EMAIL_VERIFIED),
                r.get(Users.USERS.BLOCKED_AT),
                r.get(Users.USERS.FAILED_LOGIN_ATTEMPTS),
                r.get(Users.USERS.LAST_LOGIN_AT),
                r.get(Users.USERS.VERSION),
                r.get(Users.USERS.CREATED_AT),
                r.get(Users.USERS.UPDATED_AT),
                r.get(Users.USERS.DELETED_AT)
        );
    }

    @Override
    public UserEntity insert(UserEntity entity) {
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

        dsl.insertInto(Users.USERS)
                .set(Users.USERS.ID, entity.getId())
                .set(Users.USERS.EMAIL, entity.getEmail())
                .set(Users.USERS.USERNAME, entity.getUsername())
                .set(Users.USERS.FULL_NAME, entity.getFullName())
                .set(Users.USERS.IS_ACTIVE, entity.getIsActive())
                .set(Users.USERS.IS_EMAIL_VERIFIED, entity.getIsEmailVerified())
                .set(Users.USERS.BLOCKED_AT, entity.getBlockedAt())
                .set(Users.USERS.FAILED_LOGIN_ATTEMPTS, entity.getFailedLoginAttempts())
                .set(Users.USERS.LAST_LOGIN_AT, entity.getLastLoginAt())
                .set(Users.USERS.VERSION, entity.getVersion())
                .set(Users.USERS.CREATED_AT, entity.getCreatedAt())
                .set(Users.USERS.UPDATED_AT, entity.getUpdatedAt())
                .set(Users.USERS.DELETED_AT, entity.getDeletedAt())
                .execute();

        return entity;
    }

    @Override
    public UserEntity save(UserEntity entity) {
        if (entity.getId() == null) {
            return insert(entity);
        }

        entity.incrementVersion();
        entity.touch();

        int updated = dsl.update(Users.USERS)
                .set(Users.USERS.EMAIL, entity.getEmail())
                .set(Users.USERS.USERNAME, entity.getUsername())
                .set(Users.USERS.FULL_NAME, entity.getFullName())
                .set(Users.USERS.IS_ACTIVE, entity.getIsActive())
                .set(Users.USERS.IS_EMAIL_VERIFIED, entity.getIsEmailVerified())
                .set(Users.USERS.BLOCKED_AT, entity.getBlockedAt())
                .set(Users.USERS.FAILED_LOGIN_ATTEMPTS, entity.getFailedLoginAttempts())
                .set(Users.USERS.LAST_LOGIN_AT, entity.getLastLoginAt())
                .set(Users.USERS.VERSION, entity.getVersion())
                .set(Users.USERS.UPDATED_AT, entity.getUpdatedAt())
                .set(Users.USERS.DELETED_AT, entity.getDeletedAt())
                .where(Users.USERS.ID.eq(entity.getId()))
                .execute();

        if (updated == 0) {
            return insert(entity);
        }
        return entity;
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return dsl.selectFrom(Users.USERS)
                .where(Users.USERS.EMAIL.equalIgnoreCase(email)
                        .and(Users.USERS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return dsl.selectFrom(Users.USERS)
                .where(Users.USERS.USERNAME.equalIgnoreCase(username)
                        .and(Users.USERS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public Optional<UserEntity> findByEmailOrUsername(String identifier) {
        if (identifier == null) return Optional.empty();
        Condition condition = Users.USERS.EMAIL.equalIgnoreCase(identifier)
                .or(Users.USERS.USERNAME.equalIgnoreCase(identifier));
        return dsl.selectFrom(Users.USERS)
                .where(condition.and(Users.USERS.DELETED_AT.isNull()))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return dsl.fetchExists(
                Users.USERS,
                Users.USERS.EMAIL.equalIgnoreCase(email).and(Users.USERS.DELETED_AT.isNull())
        );
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null) return false;
        return dsl.fetchExists(
                Users.USERS,
                Users.USERS.USERNAME.equalIgnoreCase(username).and(Users.USERS.DELETED_AT.isNull())
        );
    }
}
