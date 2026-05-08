package com.interswitch.verveguarddemo.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.interswitch.verveguarddemo.cache.TieredCache;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CacheHealthIndicator implements HealthIndicator {

    private final Cache<Long, Boolean> blacklistedMerchantCache;
    private final CacheManager cacheManager;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();

        // Merchant blacklist cache (Caffeine raw bean)
        CacheStats merchantStats = blacklistedMerchantCache.stats();
        details.put("merchantBlacklist.hitRate", String.format("%.2f%%", merchantStats.hitRate() * 100));
        details.put("merchantBlacklist.hitCount", merchantStats.hitCount());
        details.put("merchantBlacklist.missCount", merchantStats.missCount());
        details.put("merchantBlacklist.size", blacklistedMerchantCache.estimatedSize());

        // Fraud eval cache (Caffeine via CacheManager)
        boolean fraudEvalHealthy = addCaffeineStats(details, CacheId.FRAUD_EVALUATION.getCacheName(), "fraudEval");

        // Rate limit cache
        boolean rateLimitHealthy = addCaffeineStats(details, CacheId.RATE_LIMIT.getCacheName(), "rateLimit");

        boolean merchantHealthy = merchantStats.hitRate() >= 0.5 || merchantStats.requestCount() == 0;

        if (merchantHealthy && fraudEvalHealthy && rateLimitHealthy) {
            return Health.up().withDetails(details).build();
        }

        if (!merchantHealthy) details.put("merchantBlacklist.reason", "Hit rate below 50%");
        if (!fraudEvalHealthy) details.put("fraudEval.reason", "Hit rate below 50%");
        if (!rateLimitHealthy) details.put("rateLimit.reason", "Hit rate below 50%");

        return Health.down().withDetails(details).build();
    }

    private boolean addCaffeineStats(Map<String, Object> details, String cacheName, String prefix) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
        if (springCache == null) {
            details.put(prefix, "unavailable");
            return true; // Don't fail health check if cache is missing
        }

        // Handle TieredCache - unwrap L1 (Caffeine) for stats
        Cache<?, ?> nativeCache;
        if (springCache instanceof TieredCache tiered) {
            nativeCache = (Cache<?, ?>)
                    ((org.springframework.cache.caffeine.CaffeineCache) tiered.getL1()).getNativeCache();
        } else {
            nativeCache = (Cache<?, ?>) springCache.getNativeCache();
        }

        CacheStats stats = nativeCache.stats();
        details.put(prefix + ".hitRate", String.format("%.2f%%", stats.hitRate() * 100));
        details.put(prefix + ".hitCount", stats.hitCount());
        details.put(prefix + ".missCount", stats.missCount());
        details.put(prefix + ".size", nativeCache.estimatedSize());

        return stats.hitRate() >= 0.5 || stats.requestCount() == 0;
    }
}