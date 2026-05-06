package com.interswitch.verveguarddemo.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.interswitch.verveguarddemo.cache.TieredCache;
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
        details.put("merchantBlacklist.hitRate",  String.format("%.2f%%", merchantStats.hitRate() * 100));
        details.put("merchantBlacklist.hitCount",  merchantStats.hitCount());
        details.put("merchantBlacklist.missCount", merchantStats.missCount());
        details.put("merchantBlacklist.size",      blacklistedMerchantCache.estimatedSize());

        // Fraud eval cache (Caffeine via CacheManager)
        boolean fraudEvalHealthy = false;
        org.springframework.cache.Cache springCache = cacheManager.getCache("fraud-eval");
        if (springCache != null) {
            com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<?, ?>) springCache.getNativeCache();
            // getNativeCache() on TieredCache returns `this`, so unwrap L1
            if (nativeCache instanceof TieredCache tiered) {
                nativeCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>)
                        ((org.springframework.cache.caffeine.CaffeineCache) tiered.getL1()).getNativeCache();
            }
            CacheStats fraudStats = nativeCache.stats();
            details.put("fraudEval.hitRate",  String.format("%.2f%%", fraudStats.hitRate() * 100));
            details.put("fraudEval.hitCount",  fraudStats.hitCount());
            details.put("fraudEval.missCount", fraudStats.missCount());
            details.put("fraudEval.size",      nativeCache.estimatedSize());
            fraudEvalHealthy = fraudStats.hitRate() >= 0.5 || fraudStats.requestCount() == 0;
        } else {
            details.put("fraudEval", "unavailable");
        }

        boolean merchantHealthy = merchantStats.hitRate() >= 0.5 || merchantStats.requestCount() == 0;

        if (merchantHealthy && fraudEvalHealthy) {
            return Health.up().withDetails(details).build();
        }

        if (!merchantHealthy) details.put("merchantBlacklist.reason", "Hit rate below 50%");
        if (!fraudEvalHealthy) details.put("fraudEval.reason", "Hit rate below 50%");

        return Health.down().withDetails(details).build();
    }
}