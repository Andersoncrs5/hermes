package org.hermes.modules.authorization.ports.in;

import org.hermes.modules.authorization.application.dto.AssignRoleToUserTenantDTO;
import org.hermes.shared.result.Result;

public interface IAssignRoleToUserTenantUseCase {
    Result<Void> execute(AssignRoleToUserTenantDTO dto);
}
