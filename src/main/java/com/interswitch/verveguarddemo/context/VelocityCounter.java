package com.interswitch.verveguarddemo.context;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class VelocityCounter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "fraud:velocity:";
    private static final Duration RETENTION = Duration.ofHours(25);

    public void record(String cardHash, String transactionId) {
        String key = KEY_PREFIX + cardHash;
        double score = Instant.now().toEpochMilli();
        redisTemplate.opsForZSet().add(key, transactionId, score);
        redisTemplate.expire(key, RETENTION);
    }

    public int count(String cardHash, OffsetDateTime since) {
        String key = KEY_PREFIX + cardHash;
        double min = since.toInstant().toEpochMilli();
        Long count = redisTemplate.opsForZSet().count(key, min, Double.MAX_VALUE);
        return count != null ? count.intValue() : 0;
    }
}