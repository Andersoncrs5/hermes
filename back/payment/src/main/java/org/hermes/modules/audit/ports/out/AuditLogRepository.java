package org.hermes.modules.audit.ports.out;

import org.hermes.modules.audit.domain.entity.AuditLogEntity;

import java.util.List;

public interface AuditLogRepository {

    void save(AuditLogEntity log);

    List<AuditLogEntity> findByTenantId(Long tenantId, int offset, int limit);

    List<AuditLogEntity> findByActor(String actorType, Long actorId, int offset, int limit);
}
