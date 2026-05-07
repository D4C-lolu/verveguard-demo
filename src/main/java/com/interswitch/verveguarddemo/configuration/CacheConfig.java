package com.interswitch.verveguarddemo.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.interswitch.verveguarddemo.cache.TieredCacheManager;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    @Bean
    public Cache<Long, Boolean> blacklistedMerchantCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CacheId.FRAUD_EVALUATION.getCacheName()); // only fraud-eval gets L1
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats());
        manager.setAllowNullValues(false);
        return manager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer
                .builder()
                .enableDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class)
                                .build()
                )
                .build();

        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                )
                .disableCachingNullValues()
                .prefixCacheNameWith("");

        return RedisCacheManager
                .builder(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory))
                .cacheDefaults(defaults)
                .withCacheConfiguration(CacheId.FRAUD_EVALUATION.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheId.ACCOUNT.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.ACCOUNT_BY_NUMBER.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.CARD.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.CARD_ID_BY_HASH.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.CARD_ACTIVE_BY_HASH.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheId.MERCHANT.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CacheId.MERCHANT_VALIDATION.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.MERCHANT_ID_BY_USER.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CacheId.MERCHANT_ROLE_ID.getCacheName(),
                        defaults.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CacheId.MERCHANT_TIER_EXISTS.getCacheName(),
                        defaults.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CacheId.MERCHANT_EMAIL.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CacheId.MERCHANT_EMAIL_BY_ACCOUNT.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CacheId.MERCHANT_NAME_BY_ACCOUNT.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CacheId.BLACKLIST.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheId.BLACKLISTED_MERCHANT.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheId.TIER_CONFIG.getCacheName(),
                        defaults.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CacheId.USER.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.USER_BY_EMAIL.getCacheName(),
                        defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheId.USER_ROLE_NAME.getCacheName(),
                        defaults.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CacheId.ROLE_PERMISSIONS.getCacheName(),
                        defaults.entryTtl(Duration.ZERO))
                .withCacheConfiguration(CacheId.PERMISSIONS.getCacheName(),
                        defaults.entryTtl(Duration.ZERO))
                .build();
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return new TieredCacheManager(
                caffeineCacheManager(),
                redisCacheManager(connectionFactory),
                CacheId.FRAUD_EVALUATION.getCacheName()   // only fraud-eval gets L1+L2, everything else → Redis only
        );
    }
}