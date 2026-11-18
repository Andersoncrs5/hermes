package org.hermes.modules.authentication.ports.in;

import org.hermes.modules.identity.domain.entity.UserEntity;
import org.hermes.shared.result.Result;

import java.util.List;
import java.util.Map;

public interface ITokenService {

    Result<String> generateToken(UserEntity user, List<String> roles, List<String> permissions, Long tenantId);

    Result<Map<String, Object>> extractAllClaims(String token);

    Result<String> validateToken(String token);

    String generateRefreshTokenValue();

    String hashToken(String rawToken);
}
