package org.hermes.modules.identity.application.dto;

import java.time.OffsetDateTime;

public record UserTenantDTO(
        Long id,
        Long userId,
        Long tenantId,
        String status,
        OffsetDateTime joinedAt,
        Long invitedBy
) {
}
