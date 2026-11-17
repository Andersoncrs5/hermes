package org.hermes.shared.infrastructure.idempotency;

import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.hermes.shared.api.ResponseHTTP;
import org.hermes.shared.infrastructure.idempotency.annotation.Idempotent;

import java.util.List;

@Provider
@Idempotent
@Priority(Priorities.HEADER_DECORATOR)
public class IdempotencyFilter implements ContainerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "idempotency-key";

    @Inject
    IdempotencyService idempotencyService;

    @Inject
    Instance<RoutingContext> routingContextInstance;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        List<String> headers = requestContext.getHeaders().get(IDEMPOTENCY_HEADER);

        if (headers == null || headers.isEmpty() || headers.get(0).isBlank()) {
            abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(),
                    "Idempotency-Key header is required.");
            return;
        }

        if (headers.size() > 1) {
            abortWithResponse(requestContext, Response.Status.BAD_REQUEST.getStatusCode(),
                    "Idempotency-Key header must contain a single value.");
            return;
        }

        String idempotencyKey = headers.get(0).trim();
        String path = requestContext.getUriInfo().getPath();
        if (path.length() > 100) {
            path = path.substring(0, 100);
        }

        String consumerSource = requestContext.getMethod() + ":" + path;

        if (idempotencyService.isProcessed(idempotencyKey, consumerSource)) {
            abortWithResponse(requestContext, Response.Status.CONFLICT.getStatusCode(),
                    "Request with this Idempotency-Key has already been processed.");
            return;
        }

        String payload = null;
        if (!routingContextInstance.isUnsatisfied() && routingContextInstance.get().body() != null) {
            payload = routingContextInstance.get().body().asString();
        }

        try {
            idempotencyService.registerRequest(idempotencyKey, consumerSource, payload);
        } catch (Exception e) {
            abortWithResponse(requestContext, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());
        }
    }

    private void abortWithResponse(ContainerRequestContext requestContext, int statusCode, String message) {
        requestContext.abortWith(
                Response.status(statusCode)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(ResponseHTTP.error(message))
                        .build()
        );
    }
}
