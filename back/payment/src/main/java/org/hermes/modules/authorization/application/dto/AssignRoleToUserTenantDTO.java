package org.hermes.modules.authorization.application.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRoleToUserTenantDTO(
        @NotNull(message = "UserTenant ID is required")
        Long userTenantId,

        @NotNull(message = "Role ID is required")
        Long roleId
) {
}
