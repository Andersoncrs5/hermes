package org.hermes.modules.authentication.ports.in;

import org.hermes.shared.result.Result;

public interface ILogoutUseCase {
    Result<Void> execute(String refreshToken);
}
