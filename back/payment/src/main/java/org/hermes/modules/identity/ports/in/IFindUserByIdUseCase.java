package org.hermes.modules.identity.ports.in;

import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.shared.result.Result;

public interface IFindUserByIdUseCase {
    Result<UserDTO> execute(Long userId);
}
