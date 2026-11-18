package org.hermes.modules.identity.ports.in;

import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.shared.result.Result;

import java.util.List;

public interface IListTenantsUseCase {
    Result<List<TenantDTO>> execute(Long userId);
}
