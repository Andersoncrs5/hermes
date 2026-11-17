package org.hermes.modules.audit.adapter.out.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.infra.jooq.tables.AuditLogs;
import org.hermes.modules.audit.domain.entity.AuditLogEntity;
import org.hermes.modules.audit.ports.out.AuditLogRepository;
import org.hermes.shared.domain.id.SnowflakeIdGenerator;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class AuditLogRepositoryJooq implements AuditLogRepository {

    private final DSLContext dsl;
    private final SnowflakeIdGenerator idGenerator;

    @Inject
    public AuditLogRepositoryJooq(DSLContext dsl, SnowflakeIdGenerator idGenerator) {
        this.dsl = dsl;
        this.idGenerator = idGenerator;
    }

    private AuditLogEntity mapRecord(Record r) {
        if (r == null) return null;
        JSON beforeJson = r.get(AuditLogs.AUDIT_LOGS.BEFORE);
        JSON afterJson = r.get(AuditLogs.AUDIT_LOGS.AFTER);

        return new AuditLogEntity(
                r.get(AuditLogs.AUDIT_LOGS.ID),
                r.get(AuditLogs.AUDIT_LOGS.TENANT_ID),
                r.get(AuditLogs.AUDIT_LOGS.ACTOR_TYPE),
                r.get(AuditLogs.AUDIT_LOGS.ACTOR_ID),
                r.get(AuditLogs.AUDIT_LOGS.ACTION),
                r.get(AuditLogs.AUDIT_LOGS.RESOURCE_TYPE),
                r.get(AuditLogs.AUDIT_LOGS.RESOURCE_ID),
                beforeJson != null ? beforeJson.data() : null,
                afterJson != null ? afterJson.data() : null,
                r.get(AuditLogs.AUDIT_LOGS.IP_ADDRESS),
                r.get(AuditLogs.AUDIT_LOGS.USER_AGENT),
                r.get(AuditLogs.AUDIT_LOGS.TRACE_ID),
                r.get(AuditLogs.AUDIT_LOGS.CREATED_AT)
        );
    }

    @Override
    public void save(AuditLogEntity log) {
        if (log.getId() == null) {
            log.setId(idGenerator.nextId());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(OffsetDateTime.now());
        }

        dsl.insertInto(AuditLogs.AUDIT_LOGS)
                .set(AuditLogs.AUDIT_LOGS.ID, log.getId())
                .set(AuditLogs.AUDIT_LOGS.TENANT_ID, log.getTenantId())
                .set(AuditLogs.AUDIT_LOGS.ACTOR_TYPE, log.getActorType())
                .set(AuditLogs.AUDIT_LOGS.ACTOR_ID, log.getActorId())
                .set(AuditLogs.AUDIT_LOGS.ACTION, log.getAction())
                .set(AuditLogs.AUDIT_LOGS.RESOURCE_TYPE, log.getResourceType())
                .set(AuditLogs.AUDIT_LOGS.RESOURCE_ID, log.getResourceId())
                .set(AuditLogs.AUDIT_LOGS.BEFORE, log.getBefore() != null ? JSON.valueOf(log.getBefore()) : null)
                .set(AuditLogs.AUDIT_LOGS.AFTER, log.getAfter() != null ? JSON.valueOf(log.getAfter()) : null)
                .set(AuditLogs.AUDIT_LOGS.IP_ADDRESS, log.getIpAddress())
                .set(AuditLogs.AUDIT_LOGS.USER_AGENT, log.getUserAgent())
                .set(AuditLogs.AUDIT_LOGS.TRACE_ID, log.getTraceId())
                .set(AuditLogs.AUDIT_LOGS.CREATED_AT, log.getCreatedAt())
                .execute();
    }

    @Override
    public List<AuditLogEntity> findByTenantId(Long tenantId, int offset, int limit) {
        return dsl.selectFrom(AuditLogs.AUDIT_LOGS)
                .where(AuditLogs.AUDIT_LOGS.TENANT_ID.eq(tenantId))
                .orderBy(AuditLogs.AUDIT_LOGS.CREATED_AT.desc())
                .offset(Math.max(0, offset))
                .limit(Math.max(1, limit))
                .fetch(this::mapRecord);
    }

    @Override
    public List<AuditLogEntity> findByActor(String actorType, Long actorId, int offset, int limit) {
        return dsl.selectFrom(AuditLogs.AUDIT_LOGS)
                .where(AuditLogs.AUDIT_LOGS.ACTOR_TYPE.eq(actorType)
                        .and(AuditLogs.AUDIT_LOGS.ACTOR_ID.eq(actorId)))
                .orderBy(AuditLogs.AUDIT_LOGS.CREATED_AT.desc())
                .offset(Math.max(0, offset))
                .limit(Math.max(1, limit))
                .fetch(this::mapRecord);
    }
}
