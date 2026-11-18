package org.hermes.shared.domain.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    @DisplayName("Should generate positive 64-bit Snowflake Long IDs")
    void shouldGeneratePositiveSnowflakeIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, 1L);

        Long id1 = generator.nextId();
        Long id2 = generator.nextId();

        assertNotNull(id1);
        assertNotNull(id2);
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertTrue(id2 > id1, "IDs generated in sequence should be monotonically increasing");
    }

    @Test
    @DisplayName("Should generate unique IDs concurrently across multiple threads")
    void shouldGenerateUniqueIdsConcurrently() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2L, 1L);
        int threads = 10;
        int idsPerThread = 500;
        int totalIds = threads * idsPerThread;

        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(totalIds, generatedIds.size(), "All generated IDs across threads must be unique");
    }
}
