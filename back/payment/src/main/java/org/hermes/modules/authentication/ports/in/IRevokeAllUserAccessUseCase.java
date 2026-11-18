package org.hermes.modules.authentication.ports.in;

import org.hermes.shared.result.Result;

public interface IRevokeAllUserAccessUseCase {
    Result<Void> execute(Long userId);
}
