package org.hermes.modules.authorization.ports.in;

import org.hermes.shared.result.Result;

public interface IAssignPermissionToRoleUseCase {
    Result<Void> execute(Long roleId, Long permissionId);
}
