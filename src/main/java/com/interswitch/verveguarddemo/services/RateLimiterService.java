package com.interswitch.verveguarddemo.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.interswitch.verveguarddemo.constants.CacheId;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_MINUTES = 1;
    private static final Set<String> BYPASS_IPS = Set.of(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "::1",
            "localhost"
    );

    private final CacheManager cacheManager;

    @SuppressWarnings("unchecked")
    private Cache<String, Bucket> getBuckets() {
        var springCache = cacheManager.getCache(CacheId.RATE_LIMIT.getCacheName());
        if (springCache == null) {
            throw new IllegalStateException("Rate limit cache not configured");
        }
        return (Cache<String, Bucket>) springCache.getNativeCache();
    }

    public boolean isRateLimited(String ip) {
        if (ip == null || BYPASS_IPS.contains(ip) || isDockerNetwork(ip)) {
            return false;
        }
        Bucket bucket = getBuckets().get(ip, _ -> buildBucket());
        return !bucket.tryConsume(1);
    }

    private boolean isDockerNetwork(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            }
        }
        return false;
    }

    private Bucket buildBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_REQUESTS)
                        .refillGreedy(MAX_REQUESTS, Duration.ofMinutes(WINDOW_MINUTES))
                        .build())
                .build();
    }
}