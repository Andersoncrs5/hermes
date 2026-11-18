package org.hermes.modules.identity.ports.in;

import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.shared.result.Result;

public interface IFindTenantByIdUseCase {
    Result<TenantDTO> execute(Long tenantId);
}
