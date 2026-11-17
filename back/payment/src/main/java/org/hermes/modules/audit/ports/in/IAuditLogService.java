package org.hermes.modules.audit.ports.in;

public interface IAuditLogService {

    void log(Long tenantId, Long userId, String action, String entityName, Long entityId, String beforeJson, String afterJson, String ipAddress, String userAgent);
}
