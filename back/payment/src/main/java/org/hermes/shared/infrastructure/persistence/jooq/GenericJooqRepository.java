package org.hermes.shared.infrastructure.persistence.jooq;

import org.hermes.shared.domain.base.model.BaseEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public abstract class GenericJooqRepository<T extends BaseEntity, R extends Record, ID> implements GenericRepository<T, ID> {

    protected final DSLContext dsl;
    protected final Table<R> table;
    protected final Field<ID> idField;
    protected final Field<Long> versionField;
    protected final Field<OffsetDateTime> createdAtField;
    protected final Field<OffsetDateTime> updatedAtField;
    protected final Field<OffsetDateTime> deletedAtField;

    protected GenericJooqRepository(
            DSLContext dsl,
            Table<R> table,
            Field<ID> idField,
            Field<Long> versionField,
            Field<OffsetDateTime> createdAtField,
            Field<OffsetDateTime> updatedAtField,
            Field<OffsetDateTime> deletedAtField
    ) {
        this.dsl = dsl;
        this.table = table;
        this.idField = idField;
        this.versionField = versionField;
        this.createdAtField = createdAtField;
        this.updatedAtField = updatedAtField;
        this.deletedAtField = deletedAtField;
    }

    protected abstract T mapRecordToEntity(Record r);

    @Override
    public Optional<T> findById(ID id) {
        Condition condition = idField.eq(id);
        if (deletedAtField != null) {
            condition = condition.and(deletedAtField.isNull());
        }
        return dsl.selectFrom(table)
                .where(condition)
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public Optional<T> findByIdForUpdate(ID id) {
        Condition condition = idField.eq(id);
        if (deletedAtField != null) {
            condition = condition.and(deletedAtField.isNull());
        }
        return dsl.selectFrom(table)
                .where(condition)
                .forUpdate()
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public Optional<T> findByIdWithDeleted(ID id) {
        return dsl.selectFrom(table)
                .where(idField.eq(id))
                .fetchOptional(this::mapRecordToEntity);
    }

    @Override
    public List<T> findAll() {
        Condition condition = (deletedAtField != null) ? deletedAtField.isNull() : DSL.trueCondition();
        return dsl.selectFrom(table)
                .where(condition)
                .orderBy(idField.asc())
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public List<T> findAll(int offset, int limit) {
        Condition condition = (deletedAtField != null) ? deletedAtField.isNull() : DSL.trueCondition();
        return dsl.selectFrom(table)
                .where(condition)
                .orderBy(idField.asc())
                .offset(Math.max(0, offset))
                .limit(Math.max(1, limit))
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public List<T> findAllDeleted() {
        if (deletedAtField == null) return List.of();
        return dsl.selectFrom(table)
                .where(deletedAtField.isNotNull())
                .orderBy(idField.asc())
                .fetch(this::mapRecordToEntity);
    }

    @Override
    public boolean existsById(ID id) {
        Condition condition = idField.eq(id);
        if (deletedAtField != null) {
            condition = condition.and(deletedAtField.isNull());
        }
        return dsl.fetchExists(table, condition);
    }

    @Override
    public boolean existsAllByIds(List<ID> ids) {
        if (ids == null || ids.isEmpty()) return true;
        Condition condition = idField.in(ids);
        if (deletedAtField != null) {
            condition = condition.and(deletedAtField.isNull());
        }
        Long count = dsl.selectCount().from(table).where(condition).fetchOne(0, Long.class);
        return count != null && count == ids.size();
    }

    @Override
    public long count() {
        Condition condition = (deletedAtField != null) ? deletedAtField.isNull() : DSL.trueCondition();
        Long count = dsl.selectCount().from(table).where(condition).fetchOne(0, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countWithDeleted() {
        Long count = dsl.selectCount().from(table).fetchOne(0, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public int deleteById(ID id) {
        if (deletedAtField != null) {
            return dsl.update(table)
                    .set(deletedAtField, OffsetDateTime.now())
                    .where(idField.eq(id).and(deletedAtField.isNull()))
                    .execute();
        }
        return dsl.deleteFrom(table).where(idField.eq(id)).execute();
    }

    @Override
    public int deleteById(ID id, long expectedVersion) {
        Condition condition = idField.eq(id).and(versionField.eq(expectedVersion));
        if (deletedAtField != null) {
            condition = condition.and(deletedAtField.isNull());
            return dsl.update(table)
                    .set(deletedAtField, OffsetDateTime.now())
                    .set(versionField, expectedVersion + 1)
                    .where(condition)
                    .execute();
        }
        return dsl.deleteFrom(table).where(condition).execute();
    }

    @Override
    public int deleteAll() {
        if (deletedAtField != null) {
            return dsl.update(table)
                    .set(deletedAtField, OffsetDateTime.now())
                    .where(deletedAtField.isNull())
                    .execute();
        }
        return dsl.deleteFrom(table).execute();
    }

    @Override
    public int deleteAllById(List<ID> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        if (deletedAtField != null) {
            return dsl.update(table)
                    .set(deletedAtField, OffsetDateTime.now())
                    .where(idField.in(ids).and(deletedAtField.isNull()))
                    .execute();
        }
        return dsl.deleteFrom(table).where(idField.in(ids)).execute();
    }

    @Override
    public int restoreById(ID id) {
        if (deletedAtField == null) return 0;
        return dsl.update(table)
                .set(deletedAtField, (OffsetDateTime) null)
                .where(idField.eq(id).and(deletedAtField.isNotNull()))
                .execute();
    }

    @Override
    public int restoreAllByIds(List<ID> ids) {
        if (deletedAtField == null || ids == null || ids.isEmpty()) return 0;
        return dsl.update(table)
                .set(deletedAtField, (OffsetDateTime) null)
                .where(idField.in(ids).and(deletedAtField.isNotNull()))
                .execute();
    }

    @Override
    public int deleteForceById(ID id) {
        return dsl.deleteFrom(table).where(idField.eq(id)).execute();
    }

    @Override
    public int deleteAllForceById(List<ID> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return dsl.deleteFrom(table).where(idField.in(ids)).execute();
    }

    @Override
    public List<T> insertAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) return List.of();
        for (T entity : entities) {
            insert(entity);
        }
        return entities;
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) return List.of();
        for (T entity : entities) {
            save(entity);
        }
        return entities;
    }
}
