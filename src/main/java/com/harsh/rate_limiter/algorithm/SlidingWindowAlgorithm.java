package com.harsh.rate_limiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class SlidingWindowAlgorithm implements RateLimitAlgorithm {

    private final StringRedisTemplate redisTemplate;

    public SlidingWindowAlgorithm(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String clientId, int maxRequests, int windowSizeSeconds) {

        String key = "sliding_window:" + clientId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSizeSeconds * 1000L);

        // NOTE: This is not atomic and may fail under concurrent requests.
        // In production, use Redis Lua scripts for atomic operations.

        // Step 1: Remove all timestamps older than the window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Step 2: Count requests in current window
        Long requestCount = redisTemplate.opsForZSet().size(key);
        if (requestCount == null) requestCount = 0L;

        if (requestCount < maxRequests) {
            // Step 3: Add current timestamp
            redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
            // Step 4: Set expiry
            redisTemplate.expire(key, windowSizeSeconds * 2L, TimeUnit.SECONDS);
            return true;
        }

        return false;
    }
}