package org.hermes.modules.authorization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleDTO(
        @NotBlank(message = "Role name is required")
        @Size(max = 50)
        String name,

        String description
) {
}
