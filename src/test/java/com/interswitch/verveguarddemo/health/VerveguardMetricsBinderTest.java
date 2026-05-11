package com.interswitch.verveguarddemo.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.interswitch.verveguard.health.VerveguardHealthIndicator;
import com.interswitch.verveguarddemo.cache.TieredCache;
import com.interswitch.verveguarddemo.constants.CacheId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerveguardMetricsBinderTest {

    @Mock
    private CacheHealthIndicator cacheHealthIndicator;

    @Mock
    private VerveguardHealthIndicator verveguardHealthIndicator;

    @Mock
    private CacheManager cacheManager;

    private Cache<Long, Boolean> blacklistedMerchantCache;
    private MeterRegistry registry;
    private VerveguardMetricsBinder metricsBinder;

    @BeforeEach
    void setUp() {
        blacklistedMerchantCache = Caffeine.newBuilder().recordStats().build();
        registry = new SimpleMeterRegistry();
        metricsBinder = new VerveguardMetricsBinder(
                cacheHealthIndicator,
                verveguardHealthIndicator,
                blacklistedMerchantCache,
                cacheManager
        );
    }

    @Test
    void bindTo_registersVerveguardHealthGauge() {
        when(verveguardHealthIndicator.health()).thenReturn(Health.up().build());

        metricsBinder.bindTo(registry);

        var gauge = registry.find("verveguard.health")
                .tag("name", "fraud-gates")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void bindTo_registersVerveguardHealthGaugeDown() {
        when(verveguardHealthIndicator.health()).thenReturn(Health.down().build());

        metricsBinder.bindTo(registry);

        var gauge = registry.find("verveguard.health")
                .tag("name", "fraud-gates")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    void bindTo_registersCacheHealthGauge() {
        when(cacheHealthIndicator.health()).thenReturn(Health.up().build());

        metricsBinder.bindTo(registry);

        var gauge = registry.find("verveguard.health")
                .tag("name", "cache")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void bindTo_registersMerchantBlacklistCacheMetrics() {
        blacklistedMerchantCache.put(1L, true);
        blacklistedMerchantCache.getIfPresent(1L); // hit
        blacklistedMerchantCache.getIfPresent(2L); // miss

        metricsBinder.bindTo(registry);

        assertThat(registry.find("verveguard.cache.hit.rate")
                .tag("name", "blacklisted-merchants")
                .gauge()).isNotNull();

        assertThat(registry.find("verveguard.cache.size")
                .tag("name", "blacklisted-merchants")
                .gauge()).isNotNull();

        assertThat(registry.find("verveguard.cache.hit.count")
                .tag("name", "blacklisted-merchants")
                .gauge()).isNotNull();

        assertThat(registry.find("verveguard.cache.miss.count")
                .tag("name", "blacklisted-merchants")
                .gauge()).isNotNull();
    }

    @Test
    void bindTo_registersFraudEvalCacheMetrics() {
        metricsBinder.bindTo(registry);

        assertThat(registry.find("verveguard.cache.hit.rate")
                .tag("name", "fraud-eval")
                .gauge()).isNotNull();

        assertThat(registry.find("verveguard.cache.size")
                .tag("name", "fraud-eval")
                .gauge()).isNotNull();
    }

    @Test
    void bindTo_registersRateLimitCacheMetrics() {
        metricsBinder.bindTo(registry);

        assertThat(registry.find("verveguard.cache.hit.rate")
                .tag("name", "rate-limit")
                .gauge()).isNotNull();
    }

    @Test
    void bindTo_handlesMissingCache() {
        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(null);

        metricsBinder.bindTo(registry);

        // Gauge is registered, and when read it uses fallback empty cache
        var gauge = registry.find("verveguard.cache.hit.rate")
                .tag("name", "fraud-eval")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isNotNull(); // fallback returns 0.0 or NaN
    }

    @Test
    void bindTo_handlesTieredCache() {
        Cache<Object, Object> l1Native = Caffeine.newBuilder().recordStats().build();
        CaffeineCache l1Cache = new CaffeineCache("l1", l1Native);
        Cache<Object, Object> l2Native = Caffeine.newBuilder().recordStats().build();
        CaffeineCache l2Cache = new CaffeineCache(CacheId.FRAUD_EVALUATION.getCacheName(), l2Native);

        TieredCache tieredCache = new TieredCache(l1Cache, l2Cache);

        when(cacheManager.getCache(CacheId.FRAUD_EVALUATION.getCacheName())).thenReturn(tieredCache);

        metricsBinder.bindTo(registry);

        var gauge = registry.find("verveguard.cache.hit.rate")
                .tag("name", "fraud-eval")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isNotNull();
    }
}
