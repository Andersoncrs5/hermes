package org.hermes.modules.authorization.ports.in;

import org.hermes.modules.authorization.application.dto.CreateRoleDTO;
import org.hermes.modules.authorization.application.dto.RoleDTO;
import org.hermes.shared.result.Result;

public interface ICreateRoleUseCase {
    Result<RoleDTO> execute(CreateRoleDTO dto);
}
