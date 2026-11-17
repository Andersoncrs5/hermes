package org.hermes.modules.authorization.application.dto;

import java.time.OffsetDateTime;

public record RoleDTO(
        Long id,
        String name,
        String description,
        Boolean isSystem,
        OffsetDateTime createdAt
) {
}
