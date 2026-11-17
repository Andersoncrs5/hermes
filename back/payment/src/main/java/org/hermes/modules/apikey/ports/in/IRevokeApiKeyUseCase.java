package org.hermes.modules.apikey.ports.in;

import org.hermes.shared.result.Result;

public interface IRevokeApiKeyUseCase {
    Result<Void> execute(Long tenantId, Long apiKeyId);
}
