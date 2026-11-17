package org.hermes.modules.identity.ports.in;

import org.hermes.modules.identity.application.dto.CreateTenantDTO;
import org.hermes.modules.identity.application.dto.TenantDTO;
import org.hermes.shared.result.Result;

public interface ICreateTenantUseCase {
    Result<TenantDTO> execute(CreateTenantDTO dto, Long creatorUserId);
}
