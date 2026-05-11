package com.interswitch.verveguarddemo.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TieredCacheTest {

    private ConcurrentMapCache l1;
    private ConcurrentMapCache l2;
    private TieredCache tieredCache;

    @BeforeEach
    void setUp() {
        l1 = new ConcurrentMapCache("test-l1");
        l2 = new ConcurrentMapCache("test-l2");
        tieredCache = new TieredCache(l1, l2);
    }

    @Test
    void getName_returnsL2Name() {
        assertThat(tieredCache.getName()).isEqualTo("test-l2");
    }

    @Test
    void getNativeCache_returnsSelf() {
        assertThat(tieredCache.getNativeCache()).isSameAs(tieredCache);
    }

    @Test
    void getL1_returnsL1Cache() {
        assertThat(tieredCache.getL1()).isSameAs(l1);
    }

    // --- get(Object key) tests ---
    @Test
    void get_returnsFromL1WhenPresent() {
        l1.put("key1", "value1");

        Cache.ValueWrapper result = tieredCache.get("key1");

        assertThat(result).isNotNull();
        assertThat(result.get()).isEqualTo("value1");
    }

    @Test
    void get_returnsFromL2AndBackfillsL1WhenNotInL1() {
        l2.put("key2", "value2");

        Cache.ValueWrapper result = tieredCache.get("key2");

        assertThat(result).isNotNull();
        assertThat(result.get()).isEqualTo("value2");
        assertThat(l1.get("key2")).isNotNull();
        assertThat(l1.get("key2").get()).isEqualTo("value2");
    }

    @Test
    void get_returnsNullWhenNotInEitherCache() {
        Cache.ValueWrapper result = tieredCache.get("missing");
        assertThat(result).isNull();
    }

    // --- get(Object key, Class<T> type) tests ---
    @Test
    void getWithType_returnsFromL1WhenPresent() {
        l1.put("key", "stringValue");

        String result = tieredCache.get("key", String.class);

        assertThat(result).isEqualTo("stringValue");
    }

    @Test
    void getWithType_returnsFromL2AndBackfillsL1() {
        l2.put("key", 42);

        Integer result = tieredCache.get("key", Integer.class);

        assertThat(result).isEqualTo(42);
        assertThat(l1.get("key", Integer.class)).isEqualTo(42);
    }

    @Test
    void getWithType_returnsNullWhenMissing() {
        String result = tieredCache.get("missing", String.class);
        assertThat(result).isNull();
    }

    // --- get(Object key, Callable<T> valueLoader) tests ---
    @Test
    void getWithLoader_returnsFromL1WhenPresent() throws Exception {
        l1.put("key", "cached");
        AtomicInteger loadCount = new AtomicInteger(0);

        String result = tieredCache.get("key", () -> {
            loadCount.incrementAndGet();
            return "loaded";
        });

        assertThat(result).isEqualTo("cached");
        assertThat(loadCount.get()).isZero();
    }

    @Test
    void getWithLoader_returnsFromL2AndBackfillsL1() throws Exception {
        l2.put("key", "fromL2");
        AtomicInteger loadCount = new AtomicInteger(0);

        String result = tieredCache.get("key", () -> {
            loadCount.incrementAndGet();
            return "loaded";
        });

        assertThat(result).isEqualTo("fromL2");
        assertThat(loadCount.get()).isZero();
        assertThat(l1.get("key", String.class)).isEqualTo("fromL2");
    }

    @Test
    void getWithLoader_loadsAndStoresInBothCaches() throws Exception {
        String result = tieredCache.get("newKey", () -> "freshValue");

        assertThat(result).isEqualTo("freshValue");
        assertThat(l1.get("newKey", String.class)).isEqualTo("freshValue");
        assertThat(l2.get("newKey", String.class)).isEqualTo("freshValue");
    }

    @Test
    void getWithLoader_wrapsExceptionInValueRetrievalException() {
        Callable<String> failingLoader = () -> {
            throw new RuntimeException("Load failed");
        };

        assertThatThrownBy(() -> tieredCache.get("key", failingLoader))
                .isInstanceOf(Cache.ValueRetrievalException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void getWithLoader_onlyOneThreadLoadsPerKey() throws Exception {
        AtomicInteger loadCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tieredCache.get("contested", () -> {
                        loadCount.incrementAndGet();
                        Thread.sleep(50);
                        return "loaded";
                    });
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(loadCount.get()).isEqualTo(1);
    }

    // --- put tests ---
    @Test
    void put_storesInBothCaches() {
        tieredCache.put("key", "value");

        assertThat(l1.get("key", String.class)).isEqualTo("value");
        assertThat(l2.get("key", String.class)).isEqualTo("value");
    }

    // --- putIfAbsent tests ---
    @Test
    void putIfAbsent_storesWhenAbsent() {
        Cache.ValueWrapper result = tieredCache.putIfAbsent("key", "value");

        assertThat(result).isNull();
        assertThat(l1.get("key", String.class)).isEqualTo("value");
        assertThat(l2.get("key", String.class)).isEqualTo("value");
    }

    @Test
    void putIfAbsent_returnsExistingWhenPresent() {
        l1.put("key", "existing");

        Cache.ValueWrapper result = tieredCache.putIfAbsent("key", "new");

        assertThat(result).isNotNull();
        assertThat(result.get()).isEqualTo("existing");
    }

    // --- evict tests ---
    @Test
    void evict_removesFromBothCaches() {
        l1.put("key", "value");
        l2.put("key", "value");

        tieredCache.evict("key");

        assertThat(l1.get("key")).isNull();
        assertThat(l2.get("key")).isNull();
    }

    // --- evictIfPresent tests ---
    @Test
    void evictIfPresent_returnsTrueWhenPresentInL1() {
        l1.put("key", "value");

        boolean result = tieredCache.evictIfPresent("key");

        assertThat(result).isTrue();
        assertThat(l1.get("key")).isNull();
    }

    @Test
    void evictIfPresent_returnsTrueWhenPresentInL2Only() {
        l2.put("key", "value");

        boolean result = tieredCache.evictIfPresent("key");

        assertThat(result).isTrue();
        assertThat(l2.get("key")).isNull();
    }

    @Test
    void evictIfPresent_returnsFalseWhenAbsent() {
        boolean result = tieredCache.evictIfPresent("missing");
        assertThat(result).isFalse();
    }

    // --- clear tests ---
    @Test
    void clear_clearsAllEntriesFromBothCaches() {
        l1.put("key1", "value1");
        l1.put("key2", "value2");
        l2.put("key1", "value1");
        l2.put("key3", "value3");

        tieredCache.clear();

        assertThat(l1.get("key1")).isNull();
        assertThat(l1.get("key2")).isNull();
        assertThat(l2.get("key1")).isNull();
        assertThat(l2.get("key3")).isNull();
    }

    // --- invalidate tests ---
    @Test
    void invalidate_invalidatesBothCachesAndReturnsTrue() {
        l1.put("key", "value");
        l2.put("key", "value");

        boolean result = tieredCache.invalidate();

        assertThat(result).isTrue();
    }
}
