package com.interswitch.verveguarddemo.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.interswitch.verveguard.health.VerveguardHealthIndicator;
import com.interswitch.verveguarddemo.cache.TieredCache;
import com.interswitch.verveguarddemo.constants.CacheId;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.health.contributor.Status;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VerveguardMetricsBinder implements MeterBinder {

    private final CacheHealthIndicator cacheHealthIndicator;
    private final VerveguardHealthIndicator verveguardHealthIndicator;
    private final Cache<Long, Boolean> blacklistedMerchantCache;
    private final CacheManager cacheManager;

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {

        // --- VerveguardHealthIndicator ---
        Gauge.builder("verveguard.health", verveguardHealthIndicator,
                h -> Objects.requireNonNull(h.health()).getStatus() == Status.UP ? 1.0 : 0.0)
             .tag("name", "fraud-gates")
             .description("Fraud gates health (1=UP, 0=DOWN)")
             .register(registry);

        // --- CacheHealthIndicator overall ---
        Gauge.builder("verveguard.health", cacheHealthIndicator,
                h -> Objects.requireNonNull(h.health()).getStatus() == Status.UP ? 1.0 : 0.0)
             .tag("name", "cache")
             .description("Cache health (1=UP, 0=DOWN)")
             .register(registry);

        // --- Merchant blacklist cache (raw Caffeine bean) ---
        Gauge.builder("verveguard.cache.hit.rate", blacklistedMerchantCache,
                c -> c.stats().hitRate())
             .tag("name", "blacklisted-merchants")
             .register(registry);

        Gauge.builder("verveguard.cache.size", blacklistedMerchantCache,
                Cache::estimatedSize)
             .tag("name", "blacklisted-merchants")
             .register(registry);

        Gauge.builder("verveguard.cache.hit.count", blacklistedMerchantCache,
                c -> c.stats().hitCount())
             .tag("name", "blacklisted-merchants")
             .register(registry);

        Gauge.builder("verveguard.cache.miss.count", blacklistedMerchantCache,
                c -> c.stats().missCount())
             .tag("name", "blacklisted-merchants")
             .register(registry);

        // --- CacheManager-backed caches ---
        bindCaffeineCache(registry, CacheId.FRAUD_EVALUATION.getCacheName(), "fraud-eval");
        bindCaffeineCache(registry, CacheId.RATE_LIMIT.getCacheName(),       "rate-limit");
    }

    private void bindCaffeineCache(MeterRegistry registry, String cacheName, String tag) {
        // Gauge lambda re-resolves the cache on every scrape — safe for late init
        Gauge.builder("verveguard.cache.hit.rate", cacheManager,
                cm -> getCaffeineStats(cm, cacheName).hitRate())
             .tag("name", tag)
             .register(registry);

        Gauge.builder("verveguard.cache.size", cacheManager,
                cm -> getNativeCache(cm, cacheName).estimatedSize())
             .tag("name", tag)
             .register(registry);

        Gauge.builder("verveguard.cache.hit.count", cacheManager,
                cm -> getCaffeineStats(cm, cacheName).hitCount())
             .tag("name", tag)
             .register(registry);

        Gauge.builder("verveguard.cache.miss.count", cacheManager,
                cm -> getCaffeineStats(cm, cacheName).missCount())
             .tag("name", tag)
             .register(registry);
    }

    private CacheStats getCaffeineStats(CacheManager cm, String cacheName) {
        return getNativeCache(cm, cacheName).stats();
    }

    private Cache<?, ?> getNativeCache(CacheManager cm, String cacheName) {
        org.springframework.cache.Cache springCache = cm.getCache(cacheName);
        if (springCache == null) return Caffeine.newBuilder().build(); // empty/safe fallback
        if (springCache instanceof TieredCache tiered) {
            return ((org.springframework.cache.caffeine.CaffeineCache) tiered.getL1()).getNativeCache();
        }
        return (Cache<?, ?>) springCache.getNativeCache();
    }
}