package org.hermes.modules.authorization.application.dto;

import java.time.OffsetDateTime;

public record PermissionDTO(
        Long id,
        String name,
        String description,
        String module,
        OffsetDateTime createdAt
) {
}
