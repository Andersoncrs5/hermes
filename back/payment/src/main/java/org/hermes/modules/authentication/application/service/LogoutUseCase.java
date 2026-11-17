package org.hermes.modules.authentication.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hermes.modules.authentication.domain.entity.RefreshTokenEntity;
import org.hermes.modules.authentication.ports.in.ILogoutUseCase;
import org.hermes.modules.authentication.ports.in.ITokenService;
import org.hermes.modules.authentication.ports.out.RefreshTokenRepository;
import org.hermes.modules.authentication.ports.out.SessionRepository;
import org.hermes.shared.persistence.tx.ResultTransaction;
import org.hermes.shared.result.Result;

import java.util.Optional;

@ApplicationScoped
public class LogoutUseCase implements ILogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final ITokenService tokenService;

    @Inject
    public LogoutUseCase(
            RefreshTokenRepository refreshTokenRepository,
            SessionRepository sessionRepository,
            ITokenService tokenService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.tokenService = tokenService;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.failure("Refresh token is required", 400);
        }

        String hash = tokenService.hashToken(refreshToken);
        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByTokenHash(hash);

        if (tokenOpt.isPresent()) {
            RefreshTokenEntity token = tokenOpt.get();
            token.revoke();
            refreshTokenRepository.save(token);

            if (token.getSessionId() != null) {
                sessionRepository.deleteById(token.getSessionId());
            }
        }

        return Result.success(null);
    }
}
