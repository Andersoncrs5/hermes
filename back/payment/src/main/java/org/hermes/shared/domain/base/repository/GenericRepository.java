package org.hermes.shared.domain.base.repository;

import org.hermes.shared.domain.base.model.BaseEntity;

import java.util.List;
import java.util.Optional;

public interface GenericRepository<T extends BaseEntity, ID> {

    T save(T entity);

    T insert(T entity);

    List<T> saveAll(List<T> entities);

    List<T> insertAll(List<T> entities);

    Optional<T> findById(ID id);

    Optional<T> findByIdForUpdate(ID id);

    Optional<T> findByIdWithDeleted(ID id);

    List<T> findAll();

    List<T> findAll(int offset, int limit);

    List<T> findAllDeleted();

    boolean existsById(ID id);

    boolean existsAllByIds(List<ID> ids);

    long count();

    long countWithDeleted();

    int deleteById(ID id);

    int deleteById(ID id, long expectedVersion);

    int deleteAll();

    int deleteAllById(List<ID> ids);

    int restoreById(ID id);

    int restoreAllByIds(List<ID> ids);

    int deleteForceById(ID id);

    int deleteAllForceById(List<ID> ids);
}
