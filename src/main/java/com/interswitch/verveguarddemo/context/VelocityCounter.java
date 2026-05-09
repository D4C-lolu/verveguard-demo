package com.interswitch.verveguarddemo.context;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class VelocityCounter {

    private static final String KEY_PREFIX = "fraud:velocity:";
    private static final Duration RETENTION = Duration.ofHours(25);
    private final StringRedisTemplate redisTemplate;

    @Async
    public void record(String cardNumber, String transactionId) {
        redisTemplate.opsForZSet().add(toKey(cardNumber), transactionId,
                Instant.now().toEpochMilli());
        redisTemplate.expire(toKey(cardNumber), RETENTION);
    }

    public int count(String cardNumber, OffsetDateTime since) {
        Long count = redisTemplate.opsForZSet().count(
                toKey(cardNumber),
                since.toInstant().toEpochMilli(),
                Double.MAX_VALUE);
        return count != null ? count.intValue() : 0;
    }

    private String toKey(String cardHash) {
        // cardHash is already hashed, use it directly
        return KEY_PREFIX + cardHash;
    }
}