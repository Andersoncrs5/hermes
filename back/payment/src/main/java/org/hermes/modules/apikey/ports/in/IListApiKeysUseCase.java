package org.hermes.modules.apikey.ports.in;

import org.hermes.modules.apikey.application.dto.ApiKeyDTO;
import org.hermes.shared.result.Result;

import java.util.List;

public interface IListApiKeysUseCase {
    Result<List<ApiKeyDTO>> execute(Long tenantId);
}
