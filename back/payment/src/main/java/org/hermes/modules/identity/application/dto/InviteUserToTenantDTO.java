package org.hermes.modules.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteUserToTenantDTO(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        String roleName
) {
}
