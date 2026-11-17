package org.hermes.modules.authentication.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginUserDTO(
        @NotBlank(message = "Identifier (email or username) is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
}
