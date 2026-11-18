package org.hermes.modules.apikey.application.dto;

import java.time.OffsetDateTime;

public record ApiKeyDTO(
        Long id,
        Long tenantId,
        String name,
        String keyPrefix,
        String status,
        String environment,
        OffsetDateTime lastUsedAt,
        OffsetDateTime expiredAt,
        OffsetDateTime createdAt
) {
}
