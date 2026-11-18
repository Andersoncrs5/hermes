package org.hermes.shared.api;

import org.hermes.modules.identity.application.dto.UserDTO;

import java.time.OffsetDateTime;
import java.util.List;

public record TokenResponse(
        String token,
        OffsetDateTime expToken,
        String refreshToken,
        OffsetDateTime expRefreshToken,
        UserDTO user,
        List<String> roles
) {
}
