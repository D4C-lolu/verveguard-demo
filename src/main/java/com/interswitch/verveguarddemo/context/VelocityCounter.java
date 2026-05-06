package com.interswitch.verveguarddemo.context;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class VelocityCounter {

    private final StringRedisTemplate redisTemplate;

    private static final String   KEY_PREFIX = "fraud:velocity:";
    private static final Duration RETENTION  = Duration.ofHours(25);

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

    private String toKey(String cardNumber) {
        return KEY_PREFIX + DigestUtils.sha256Hex(cardNumber);
    }
}