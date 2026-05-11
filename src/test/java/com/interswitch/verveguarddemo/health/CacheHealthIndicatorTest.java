package com.interswitch.verveguarddemo.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.interswitch.verveguarddemo.cache.TieredCache;
import com.interswitch.verveguarddemo.constants.CacheId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheHealthIndicatorTest {

    @Mock
    private CacheManager cacheManager;

    private Cache<Long, Boolean> blacklistedMerchantCache;
    private CacheHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        blacklistedMerchantCache = Caffeine.newBuilder()
                .recordStats()
                .build();
        healthIndicator = new CacheHealthIndicator(blacklistedMerchantCache, cacheManager);
    }

    @Test
    void health_returnsUpWhenAllCachesHealthy() {
        // Setup caches with good hit rates (no requests = healthy)
        Cache<Object, Object> fraudEvalNative = Caffeine.newBuilder().recordStats().build();
        Cache<Object, Object> rateLimitNative = Caffeine.newBuilder().recordStats().build();

        CaffeineCache fraudEvalCache = new CaffeineCache(CacheId.FRAUD_EVALUATION.getCacheName(), fraudEvalNative);
        CaffeineCache rateLimitCache = new CaffeineCache(CacheId.RATE_LIMIT.getCacheName(), rateLimitNative);

        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(fraudEvalCache);
        when(cacheManager.getCache(CacheId.RATE_LIMIT.getCacheName())).thenReturn(rateLimitCache);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("merchantBlacklist.hitRate");
        assertThat(health.getDetails()).containsKey("fraudEval.hitRate");
        assertThat(health.getDetails()).containsKey("rateLimit.hitRate");
    }

    @Test
    void health_returnsDownWhenMerchantCacheHitRateLow() {
        // Simulate low hit rate on merchant cache
        blacklistedMerchantCache.get(1L, k -> true); // miss
        blacklistedMerchantCache.get(2L, k -> true); // miss
        blacklistedMerchantCache.get(3L, k -> true); // miss

        Cache<Object, Object> fraudEvalNative = Caffeine.newBuilder().recordStats().build();
        Cache<Object, Object> rateLimitNative = Caffeine.newBuilder().recordStats().build();

        CaffeineCache fraudEvalCache = new CaffeineCache(CacheId.FRAUD_EVALUATION.getCacheName(), fraudEvalNative);
        CaffeineCache rateLimitCache = new CaffeineCache(CacheId.RATE_LIMIT.getCacheName(), rateLimitNative);

        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(fraudEvalCache);
        when(cacheManager.getCache(CacheId.RATE_LIMIT.getCacheName())).thenReturn(rateLimitCache);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("merchantBlacklist.reason");
    }

    @Test
    void health_handlesNullCache() {
        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(null);
        when(cacheManager.getCache(CacheId.RATE_LIMIT.getCacheName())).thenReturn(null);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("fraudEval")).isEqualTo("unavailable");
        assertThat(health.getDetails().get("rateLimit")).isEqualTo("unavailable");
    }

    @Test
    void health_handlesTieredCache() {
        Cache<Object, Object> l1Native = Caffeine.newBuilder().recordStats().build();
        CaffeineCache l1Cache = new CaffeineCache("l1", l1Native);
        Cache<Object, Object> l2Native = Caffeine.newBuilder().recordStats().build();
        CaffeineCache l2Cache = new CaffeineCache(CacheId.FRAUD_EVALUATION.getCacheName(), l2Native);

        TieredCache tieredCache = new TieredCache(l1Cache, l2Cache);

        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(tieredCache);

        Cache<Object, Object> rateLimitNative = Caffeine.newBuilder().recordStats().build();
        CaffeineCache rateLimitCache = new CaffeineCache(CacheId.RATE_LIMIT.getCacheName(), rateLimitNative);
        when(cacheManager.getCache(CacheId.RATE_LIMIT.getCacheName())).thenReturn(rateLimitCache);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("fraudEval.hitRate");
    }

    @Test
    void health_includesAllMetrics() {
        Cache<Object, Object> fraudEvalNative = Caffeine.newBuilder().recordStats().build();
        Cache<Object, Object> rateLimitNative = Caffeine.newBuilder().recordStats().build();

        CaffeineCache fraudEvalCache = new CaffeineCache(CacheId.FRAUD_EVALUATION.getCacheName(), fraudEvalNative);
        CaffeineCache rateLimitCache = new CaffeineCache(CacheId.RATE_LIMIT.getCacheName(), rateLimitNative);

        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(fraudEvalCache);
        when(cacheManager.getCache(CacheId.RATE_LIMIT.getCacheName())).thenReturn(rateLimitCache);

        Health health = healthIndicator.health();

        // Merchant blacklist metrics
        assertThat(health.getDetails()).containsKey("merchantBlacklist.hitRate");
        assertThat(health.getDetails()).containsKey("merchantBlacklist.hitCount");
        assertThat(health.getDetails()).containsKey("merchantBlacklist.missCount");
        assertThat(health.getDetails()).containsKey("merchantBlacklist.size");

        // Fraud eval metrics
        assertThat(health.getDetails()).containsKey("fraudEval.hitRate");
        assertThat(health.getDetails()).containsKey("fraudEval.hitCount");
        assertThat(health.getDetails()).containsKey("fraudEval.missCount");
        assertThat(health.getDetails()).containsKey("fraudEval.size");

        // Rate limit metrics
        assertThat(health.getDetails()).containsKey("rateLimit.hitRate");
        assertThat(health.getDetails()).containsKey("rateLimit.hitCount");
        assertThat(health.getDetails()).containsKey("rateLimit.missCount");
        assertThat(health.getDetails()).containsKey("rateLimit.size");
    }
}
