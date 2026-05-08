package com.interswitch.verveguarddemo.cache;

import org.jspecify.annotations.NonNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TieredCacheManager implements CacheManager {

    private final CaffeineCacheManager l1;
    private final RedisCacheManager l2;
    private final Set<String> tieredCacheNames;
    private final Set<String> l1OnlyCacheNames;
    private final Map<String, TieredCache> cache = new ConcurrentHashMap<>();

    public TieredCacheManager(CaffeineCacheManager l1, RedisCacheManager l2,
                              Set<String> tieredCacheNames, Set<String> l1OnlyCacheNames) {
        this.l1 = l1;
        this.l2 = l2;
        this.tieredCacheNames = tieredCacheNames;
        this.l1OnlyCacheNames = l1OnlyCacheNames;
    }

    @Override
    public org.springframework.cache.Cache getCache(@NonNull String name) {
        // Tiered: L1 + L2
        if (tieredCacheNames.contains(name)) {
            return cache.computeIfAbsent(name, k ->
                    new TieredCache(l1.getCache(k), l2.getCache(k))
            );
        }
        // L1-only: Caffeine only (e.g., rate limiting)
        if (l1OnlyCacheNames.contains(name)) {
            return l1.getCache(name);
        }
        // Default: L2 only (Redis)
        return l2.getCache(name);
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        Set<String> names = new HashSet<>(l2.getCacheNames());
        names.addAll(tieredCacheNames);
        names.addAll(l1OnlyCacheNames);
        return Collections.unmodifiableSet(names);
    }
}