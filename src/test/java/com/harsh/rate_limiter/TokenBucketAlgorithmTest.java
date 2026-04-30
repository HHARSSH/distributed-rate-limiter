package com.harsh.rate_limiter;

import com.harsh.rate_limiter.algorithm.TokenBucketAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenBucketAlgorithmTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TokenBucketAlgorithm tokenBucketAlgorithm;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBucketAlgorithm = new TokenBucketAlgorithm(redisTemplate);
    }

    @Test
    void firstRequest_shouldBeAllowed() {
        // First request - no data in Redis yet
        when(valueOperations.get(contains("tokens"))).thenReturn(null);
        when(valueOperations.get(contains("timestamp"))).thenReturn(null);

        boolean result = tokenBucketAlgorithm.isAllowed("client1", 5, 60);

        assertTrue(result, "First request should always be allowed");
    }

    @Test
    void requestWithFullBucket_shouldBeAllowed() {
        // Bucket is full - 5 tokens available
        when(valueOperations.get(contains("tokens"))).thenReturn("5.0");
        when(valueOperations.get(contains("timestamp")))
                .thenReturn(String.valueOf(System.currentTimeMillis() / 1000));

        boolean result = tokenBucketAlgorithm.isAllowed("client1", 5, 60);

        assertTrue(result, "Request with full bucket should be allowed");
    }

    @Test
    void requestWithEmptyBucket_shouldBeBlocked() {
        // Bucket is empty - 0 tokens, request just made
        when(valueOperations.get(contains("tokens"))).thenReturn("0.0");
        when(valueOperations.get(contains("timestamp")))
                .thenReturn(String.valueOf(System.currentTimeMillis() / 1000));

        boolean result = tokenBucketAlgorithm.isAllowed("client1", 5, 60);

        assertFalse(result, "Request with empty bucket should be blocked");
    }

    @Test
    void requestAfterRefill_shouldBeAllowed() {
        // Bucket was empty but enough time has passed to refill
        when(valueOperations.get(contains("tokens"))).thenReturn("0.0");
        // Set timestamp to 60 seconds ago - full refill should have happened
        when(valueOperations.get(contains("timestamp")))
                .thenReturn(String.valueOf((System.currentTimeMillis() / 1000) - 60));

        boolean result = tokenBucketAlgorithm.isAllowed("client1", 5, 60);

        assertTrue(result, "Request after full refill period should be allowed");
    }
}