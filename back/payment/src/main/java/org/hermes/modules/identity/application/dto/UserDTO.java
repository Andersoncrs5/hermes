package org.hermes.modules.identity.application.dto;

import java.time.OffsetDateTime;

public record UserDTO(
        Long id,
        String email,
        String username,
        String fullName,
        Boolean isActive,
        Boolean emailVerified,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
