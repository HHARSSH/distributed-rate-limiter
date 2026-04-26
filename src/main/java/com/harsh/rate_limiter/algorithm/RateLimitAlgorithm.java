package com.harsh.rate_limiter.algorithm;

public interface RateLimitAlgorithm {
    boolean isAllowed(String clientId, int maxRequests, int windowSizeSeconds);
}