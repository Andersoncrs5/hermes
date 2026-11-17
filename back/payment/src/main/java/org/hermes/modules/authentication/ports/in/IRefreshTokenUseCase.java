package org.hermes.modules.authentication.ports.in;

import org.hermes.modules.authentication.application.dto.RefreshTokenRequestDTO;
import org.hermes.shared.api.TokenResponse;
import org.hermes.shared.result.Result;

public interface IRefreshTokenUseCase {
    Result<TokenResponse> execute(RefreshTokenRequestDTO dto, String ipAddress, String userAgent);
}
