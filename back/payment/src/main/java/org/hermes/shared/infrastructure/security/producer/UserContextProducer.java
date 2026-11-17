package org.hermes.shared.infrastructure.security.producer;

import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hermes.shared.infrastructure.security.annotations.TenantId;
import org.hermes.shared.infrastructure.security.annotations.UserId;
import org.hermes.shared.infrastructure.security.context.TenantSecurityContext;

@RequestScoped
public class UserContextProducer {

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantSecurityContext tenantSecurityContext;

    @Produces
    @Dependent
    @UserId
    public Long produceUserId() {
        if (tenantSecurityContext != null && tenantSecurityContext.getUserId() != null) {
            return tenantSecurityContext.getUserId();
        }

        String sub = jwt != null ? jwt.getSubject() : null;
        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("Subject claim (sub) missing in authentication token");
        }

        try {
            return Long.valueOf(sub);
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Invalid user ID format in token subject: " + sub);
        }
    }

    @Produces
    @Dependent
    @TenantId
    public Long produceTenantId() {
        if (tenantSecurityContext != null && tenantSecurityContext.getTenantId() != null) {
            return tenantSecurityContext.getTenantId();
        }
        return null;
    }
}
