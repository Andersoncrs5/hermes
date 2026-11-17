package org.hermes.modules.audit.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.audit.domain.entity.AuditLogEntity;
import org.hermes.modules.audit.ports.in.IAuditLogService;
import org.hermes.modules.audit.ports.out.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

@ApplicationScoped
public class AuditLogService implements IAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    @Inject
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(Long tenantId, Long userId, String action, String entityName, Long entityId, String beforeJson, String afterJson, String ipAddress, String userAgent) {
        try {
            AuditLogEntity audit = new AuditLogEntity();
            audit.setTenantId(tenantId);
            audit.setActorType("USER");
            audit.setActorId(userId);
            audit.setAction(action != null ? action : "UNKNOWN");
            audit.setResourceType(entityName != null ? entityName : "UNKNOWN");
            audit.setResourceId(entityId != null ? entityId.toString() : null);
            audit.setBefore(beforeJson);
            audit.setAfter(afterJson);
            audit.setIpAddress(ipAddress);
            audit.setUserAgent(userAgent);
            audit.setCreatedAt(OffsetDateTime.now());

            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to persist audit log entry: {}", e.getMessage(), e);
        }
    }
}
