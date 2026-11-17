package org.hermes.modules.authentication.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authentication.ports.in.IRevokeAllUserAccessUseCase;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

@ApplicationScoped
public class RevokeAllUserAccessUseCase implements IRevokeAllUserAccessUseCase {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Inject
    public RevokeAllUserAccessUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(Long userId) {
        if (userId == null) {
            return Result.failure("User ID is required", 400);
        }

        sessionRepository.revokeAllByUserId(userId, "USER_LOGOUT_ALL");
        refreshTokenRepository.revokeAllByUserId(userId);

        return Result.success(null);
    }
}
