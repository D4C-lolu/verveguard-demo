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
    private final Map<String, TieredCache> cache = new ConcurrentHashMap<>();

    public TieredCacheManager(CaffeineCacheManager l1, RedisCacheManager l2, String... tieredCacheNames) {
        this.l1 = l1;
        this.l2 = l2;
        this.tieredCacheNames = Set.of(tieredCacheNames);
    }

    @Override
    public org.springframework.cache.Cache getCache(@NonNull String name) {
        if (tieredCacheNames.contains(name)) {
            return cache.computeIfAbsent(name, k ->
                    new TieredCache(l1.getCache(k), l2.getCache(k))
            );
        }
        // non-tiered caches go straight to Redis
        return l2.getCache(name);
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        Set<String> names = new HashSet<>(l2.getCacheNames());
        names.addAll(tieredCacheNames);
        return Collections.unmodifiableSet(names);
    }
}