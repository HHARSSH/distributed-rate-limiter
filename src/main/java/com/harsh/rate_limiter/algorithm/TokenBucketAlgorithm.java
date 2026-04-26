package com.harsh.rate_limiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class TokenBucketAlgorithm implements RateLimitAlgorithm {

    private final StringRedisTemplate redisTemplate;

    public TokenBucketAlgorithm(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String clientId, int maxRequests, int windowSizeSeconds) {
        String tokenKey = "token_bucket:tokens:" + clientId;
        String timeKey = "token_bucket:timestamp:" + clientId;

        long now = Instant.now().getEpochSecond();

        // Get current tokens and last refill time
        // NOTE: This is not atomic and may fail under concurrent requests.
// In production, use Redis Lua scripts for atomic operations.
        String tokensStr = redisTemplate.opsForValue().get(tokenKey);
        String timeStr = redisTemplate.opsForValue().get(timeKey);

        double currentTokens;
        long lastRefillTime;

        if (tokensStr == null || timeStr == null) {
            // First request - initialize bucket
            currentTokens = maxRequests;
            lastRefillTime = now;
        } else {
            currentTokens = Double.parseDouble(tokensStr);
            lastRefillTime = Long.parseLong(timeStr);
        }

        // Calculate tokens to add based on time elapsed
        double refillRate = (double) maxRequests / windowSizeSeconds;
        long elapsedSeconds = now - lastRefillTime;
        double tokensToAdd = elapsedSeconds * refillRate;

        // Add tokens but don't exceed max capacity
        currentTokens = Math.min(maxRequests, currentTokens + tokensToAdd);

        if (currentTokens >= 1) {
            // Allow request - deduct one token
            currentTokens -= 1;
            redisTemplate.opsForValue().set(tokenKey, String.valueOf(currentTokens), windowSizeSeconds * 2L, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(timeKey, String.valueOf(now), windowSizeSeconds * 2L, TimeUnit.SECONDS);
            return true;
        }

        // Reject request
        return false;
    }
}