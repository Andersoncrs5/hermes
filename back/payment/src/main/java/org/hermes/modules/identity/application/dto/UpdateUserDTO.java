package org.hermes.modules.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserDTO(
        @NotBlank(message = "Full name is required")
        String fullName
) {
}
