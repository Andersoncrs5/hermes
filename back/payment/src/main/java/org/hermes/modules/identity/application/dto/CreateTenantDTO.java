package org.hermes.modules.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantDTO(
        @NotBlank(message = "Tenant name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Tenant slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
        @Size(min = 2, max = 100)
        String slug,

        @Size(min = 2, max = 2, message = "Country code must be ISO 3166-1 alpha-2")
        String countryCode
) {
}
