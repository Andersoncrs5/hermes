package org.hermes.modules.apikey.application.dto;

import java.time.OffsetDateTime;

public record ApiKeyResponseDTO(
        Long id,
        Long tenantId,
        String name,
        String keyPrefix,
        String secretKey,
        String environment,
        OffsetDateTime expiredAt,
        OffsetDateTime createdAt
) {
}
