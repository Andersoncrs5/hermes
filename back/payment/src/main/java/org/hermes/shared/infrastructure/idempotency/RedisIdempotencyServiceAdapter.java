package org.hermes.shared.infrastructure.idempotency;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RedisIdempotencyServiceAdapter implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyServiceAdapter.class);
    private static final Duration TTL = Duration.ofHours(24);

    @Inject
    Instance<RedisDataSource> redisDataSourceInstance;

    private ValueCommands<String, String> valueCommands;
    private final Map<String, String> memoryFallback = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        try {
            if (!redisDataSourceInstance.isUnsatisfied()) {
                this.valueCommands = redisDataSourceInstance.get().value(String.class);
            }
        } catch (Exception e) {
            log.warn("Redis DataSource initialization failed, using in-memory idempotency fallback: {}", e.getMessage());
        }
    }

    @Override
    public boolean isProcessed(String idempotencyKey, String consumerSource) {
        String key = buildKey(idempotencyKey, consumerSource);
        try {
            if (valueCommands != null) {
                String existing = valueCommands.get(key);
                return existing != null;
            }
        } catch (Exception e) {
            log.warn("Redis error on idempotency check, falling back to memory: {}", e.getMessage());
        }
        return memoryFallback.containsKey(key);
    }

    @Override
    public void registerRequest(String idempotencyKey, String consumerSource, String payload) {
        String key = buildKey(idempotencyKey, consumerSource);
        String value = payload != null ? payload : "PROCESSED";
        try {
            if (valueCommands != null) {
                valueCommands.setex(key, TTL.toSeconds(), value);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis error on idempotency register, falling back to memory: {}", e.getMessage());
        }
        memoryFallback.put(key, value);
    }

    private String buildKey(String idempotencyKey, String consumerSource) {
        return "hermes:idempotency:" + consumerSource + ":" + idempotencyKey;
    }
}
