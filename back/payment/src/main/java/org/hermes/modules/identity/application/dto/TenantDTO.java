package org.hermes.modules.identity.application.dto;

import java.time.OffsetDateTime;

public record TenantDTO(
        Long id,
        String name,
        String slug,
        String status,
        String countryCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
