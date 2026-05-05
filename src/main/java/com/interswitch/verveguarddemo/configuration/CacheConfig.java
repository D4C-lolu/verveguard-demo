package com.interswitch.verveguarddemo.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static tools.jackson.databind.DefaultTyping.NON_FINAL_AND_ENUMS;

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
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper cacheMapper = objectMapper
                .rebuild()
                .activateDefaultTyping(
                        typeValidator,
                        NON_FINAL_AND_ENUMS,
                        JsonTypeInfo.As.PROPERTY
                )
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer =
                new GenericJacksonJsonRedisSerializer(cacheMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        // Values are stored as JSON with embedded type hints.
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues()
                .prefixCacheNameWith("");

        return RedisCacheManager
                .builder(
                        // nonLockingRedisCacheWriter: no distributed lock on cache writes.
                        // Faster throughput. Acceptable for typical cache-aside patterns
                        // where a brief thundering herd on a cold key is tolerable.
                        // Use RedisCacheWriter.lockingRedisCacheWriter(connectionFactory)
                        // if you need strict putIfAbsent guarantees.
                        RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory)
                )
                .cacheDefaults(defaults)
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
}