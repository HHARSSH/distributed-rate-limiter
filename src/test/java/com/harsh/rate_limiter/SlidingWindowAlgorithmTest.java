package com.harsh.rate_limiter;

import com.harsh.rate_limiter.algorithm.SlidingWindowAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SlidingWindowAlgorithmTest {

    private StringRedisTemplate redisTemplate;
    private ZSetOperations<String, String> zSetOperations;
    private SlidingWindowAlgorithm slidingWindowAlgorithm;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        zSetOperations = Mockito.mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        slidingWindowAlgorithm = new SlidingWindowAlgorithm(redisTemplate);
    }

    @Test
    void firstRequest_shouldBeAllowed() {
        // No previous requests in window
        when(zSetOperations.size(anyString())).thenReturn(0L);

        boolean result = slidingWindowAlgorithm.isAllowed("client1", 5, 60);

        assertTrue(result, "First request should always be allowed");
    }

    @Test
    void requestUnderLimit_shouldBeAllowed() {
        // 4 requests already in window, limit is 5
        when(zSetOperations.size(anyString())).thenReturn(4L);

        boolean result = slidingWindowAlgorithm.isAllowed("client1", 5, 60);

        assertTrue(result, "Request under limit should be allowed");
    }

    @Test
    void requestAtLimit_shouldBeBlocked() {
        // 5 requests already in window, limit is 5
        when(zSetOperations.size(anyString())).thenReturn(5L);

        boolean result = slidingWindowAlgorithm.isAllowed("client1", 5, 60);

        assertFalse(result, "Request at limit should be blocked");
    }

    @Test
    void requestOverLimit_shouldBeBlocked() {
        // 10 requests already in window, limit is 5
        when(zSetOperations.size(anyString())).thenReturn(10L);

        boolean result = slidingWindowAlgorithm.isAllowed("client1", 5, 60);

        assertFalse(result, "Request over limit should be blocked");
    }

    @Test
    void nullCount_shouldBeAllowed() {
        // Redis returns null for size (empty set)
        when(zSetOperations.size(anyString())).thenReturn(null);

        boolean result = slidingWindowAlgorithm.isAllowed("client1", 5, 60);

        assertTrue(result, "Null count should be treated as 0 and allowed");
    }
}