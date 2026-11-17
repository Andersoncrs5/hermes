package org.hermes.modules.authentication.ports.in;

import org.hermes.modules.authentication.application.dto.LoginUserDTO;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.result.Result;

public interface ILoginUserUseCase {
    Result<TokenResponse> execute(LoginUserDTO dto, String ipAddress, String userAgent);
}
