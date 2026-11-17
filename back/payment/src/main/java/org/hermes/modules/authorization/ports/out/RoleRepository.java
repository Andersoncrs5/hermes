package org.hermes.modules.authorization.ports.out;

import org.hermes.modules.authorization.domain.entity.RoleEntity;
import org.hermes.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends GenericRepository<RoleEntity, Long> {

    Optional<RoleEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<RoleEntity> findBySlugs(List<String> slugs);
}
