package org.hermes.modules.authentication.ports.in;

import org.hermes.modules.identity.application.dto.CreateUserDTO;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.result.Result;

public interface IRegisterUserUseCase {
    Result<TokenResponse> execute(CreateUserDTO dto, String ipAddress, String userAgent);
}
