package org.hermes.modules.authorization.ports.in;

import org.hermes.modules.authorization.application.dto.PermissionDTO;
import org.hermes.shared.result.Result;

import java.util.List;

public interface IListPermissionsUseCase {
    Result<List<PermissionDTO>> execute();
}
