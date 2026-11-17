package org.hermes.modules.identity.ports.in;

import org.hermes.modules.identity.application.dto.InviteUserToTenantDTO;
import org.hermes.modules.identity.application.dto.UserTenantDTO;
import org.hermes.shared.result.Result;

public interface IInviteUserToTenantUseCase {
    Result<UserTenantDTO> execute(Long tenantId, InviteUserToTenantDTO dto, Long inviterUserId);
}
