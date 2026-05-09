package com.interswitch.verveguarddemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserIpHistoryService {

    private static final String KEY_PREFIX = "fraud:ip:";
    private static final int MAX_IPS = 10;
    private static final Duration TTL = Duration.ofDays(30);
    private final StringRedisTemplate redisTemplate;

    public Set<String> getRecentIps(String merchantId) {
        if (merchantId == null) {
            return Set.of();
        }
        try {
            String key = buildKey(merchantId);
            List<String> ips = redisTemplate.opsForList().range(key, 0, MAX_IPS - 1);
            return ips != null ? new HashSet<>(ips) : Set.of();
        } catch (Exception e) {
            log.warn("Failed to get IP history for merchant: {}", merchantId, e);
            return Set.of();
        }
    }

    public void recordIp(String accountId, String ipAddress) {
        if (accountId == null || ipAddress == null) return;
        try {
            String key = buildKey(accountId);
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] k = key.getBytes();
                byte[] v = ipAddress.getBytes();
                connection.listCommands().lRem(k, 1, v);
                connection.listCommands().lPush(k, v);
                connection.listCommands().lTrim(k, 0, MAX_IPS - 1);
                connection.keyCommands().expire(k, TTL.getSeconds());
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to record IP {} for account: {}", ipAddress, accountId, e);
        }
    }

    @Async
    public void recordIpAsync(String accountId, String ipAddress) {
        recordIp(accountId, ipAddress);
    }

    private String buildKey(String accountId) {
        return KEY_PREFIX + accountId;
    }
}
