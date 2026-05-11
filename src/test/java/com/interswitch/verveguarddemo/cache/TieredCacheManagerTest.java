package com.interswitch.verveguarddemo.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TieredCacheManagerTest {

    @Mock
    private CaffeineCacheManager l1Manager;

    @Mock
    private RedisCacheManager l2Manager;

    private TieredCacheManager tieredCacheManager;

    @BeforeEach
    void setUp() {
        Set<String> tieredCacheNames = Set.of("fraudEval", "staticData");
        Set<String> l1OnlyCacheNames = Set.of("rateLimit");

        tieredCacheManager = new TieredCacheManager(l1Manager, l2Manager, tieredCacheNames, l1OnlyCacheNames);
    }

    @Test
    void getCache_returnsTieredCacheForTieredName() {
        ConcurrentMapCache l1Cache = new ConcurrentMapCache("fraudEval");
        ConcurrentMapCache l2Cache = new ConcurrentMapCache("fraudEval");
        when(l1Manager.getCache("fraudEval")).thenReturn(l1Cache);
        when(l2Manager.getCache("fraudEval")).thenReturn(l2Cache);

        var cache = tieredCacheManager.getCache("fraudEval");

        assertThat(cache).isInstanceOf(TieredCache.class);
        assertThat(cache.getName()).isEqualTo("fraudEval");
    }

    @Test
    void getCache_returnsSameTieredCacheInstanceOnSubsequentCalls() {
        ConcurrentMapCache l1Cache = new ConcurrentMapCache("fraudEval");
        ConcurrentMapCache l2Cache = new ConcurrentMapCache("fraudEval");
        when(l1Manager.getCache("fraudEval")).thenReturn(l1Cache);
        when(l2Manager.getCache("fraudEval")).thenReturn(l2Cache);

        var cache1 = tieredCacheManager.getCache("fraudEval");
        var cache2 = tieredCacheManager.getCache("fraudEval");

        assertThat(cache1).isSameAs(cache2);
    }

    @Test
    void getCache_returnsL1OnlyCacheForL1OnlyName() {
        ConcurrentMapCache l1Cache = new ConcurrentMapCache("rateLimit");
        when(l1Manager.getCache("rateLimit")).thenReturn(l1Cache);

        var cache = tieredCacheManager.getCache("rateLimit");

        assertThat(cache).isSameAs(l1Cache);
        assertThat(cache).isNotInstanceOf(TieredCache.class);
    }

    @Test
    void getCache_returnsL2CacheForUnknownName() {
        ConcurrentMapCache l2Cache = new ConcurrentMapCache("otherCache");
        when(l2Manager.getCache("otherCache")).thenReturn(l2Cache);

        var cache = tieredCacheManager.getCache("otherCache");

        assertThat(cache).isSameAs(l2Cache);
    }

    @Test
    void getCacheNames_returnsAllCacheNames() {
        when(l2Manager.getCacheNames()).thenReturn(Set.of("redisCache1", "redisCache2"));

        Collection<String> names = tieredCacheManager.getCacheNames();

        assertThat(names).containsExactlyInAnyOrder(
                "redisCache1", "redisCache2",
                "fraudEval", "staticData",
                "rateLimit"
        );
    }

    @Test
    void getCacheNames_returnsUnmodifiableSet() {
        when(l2Manager.getCacheNames()).thenReturn(Set.of());

        Collection<String> names = tieredCacheManager.getCacheNames();

        assertThat(names).isUnmodifiable();
    }
}
