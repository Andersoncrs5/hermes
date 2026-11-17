package org.hermes.modules.authorization.ports.in;

import org.hermes.modules.authorization.application.dto.RoleDTO;
import org.hermes.shared.result.Result;

import java.util.List;

public interface IListRolesUseCase {
    Result<List<RoleDTO>> execute();
}
