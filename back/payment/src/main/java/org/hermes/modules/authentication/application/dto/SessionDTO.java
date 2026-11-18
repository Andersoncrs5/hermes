package org.hermes.modules.authentication.application.dto;

import java.time.OffsetDateTime;

public record SessionDTO(
        Long id,
        Long userId,
        String ipAddress,
        String userAgent,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
}
