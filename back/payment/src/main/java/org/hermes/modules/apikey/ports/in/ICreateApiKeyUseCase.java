package org.hermes.modules.apikey.ports.in;

import org.hermes.modules.apikey.application.dto.ApiKeyResponseDTO;
import org.hermes.modules.apikey.application.dto.CreateApiKeyDTO;
import org.hermes.shared.result.Result;

public interface ICreateApiKeyUseCase {
    Result<ApiKeyResponseDTO> execute(Long tenantId, Long userId, CreateApiKeyDTO dto);
}
