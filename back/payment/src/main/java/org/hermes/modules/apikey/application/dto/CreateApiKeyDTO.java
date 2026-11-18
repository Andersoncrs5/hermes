package org.hermes.modules.apikey.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateApiKeyDTO(
        @NotBlank(message = "API key name is required")
        @Size(max = 100)
        String name,

        @Pattern(regexp = "^(TEST|LIVE)$", message = "Environment must be TEST or LIVE")
        String environment,

        List<Long> permissionIds
) {
}
