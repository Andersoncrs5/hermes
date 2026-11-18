package org.hermes.shared.infrastructure.idempotency;

public interface IdempotencyService {

    boolean isProcessed(String idempotencyKey, String consumerSource);

    void registerRequest(String idempotencyKey, String consumerSource, String payload);
}
