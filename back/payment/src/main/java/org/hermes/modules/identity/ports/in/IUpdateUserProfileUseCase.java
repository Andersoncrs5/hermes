package org.hermes.modules.identity.ports.in;

import org.hermes.modules.identity.application.dto.UpdateUserDTO;
import org.hermes.modules.identity.application.dto.UserDTO;
import org.hermes.shared.result.Result;

public interface IUpdateUserProfileUseCase {
    Result<UserDTO> execute(Long userId, UpdateUserDTO dto);
}
