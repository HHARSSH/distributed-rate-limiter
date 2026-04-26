package com.harsh.rate_limiter.service;
import com.harsh.rate_limiter.algorithm.RateLimitAlgorithm;
import com.harsh.rate_limiter.model.RateLimitResponse;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final RateLimitAlgorithm algorithm;

    // Default limits
    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_SIZE_SECONDS = 60;

    public RateLimiterService(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public RateLimitResponse checkLimit(String clientId) {
        boolean allowed = algorithm.isAllowed(clientId, MAX_REQUESTS, WINDOW_SIZE_SECONDS);

        if (allowed) {
            return RateLimitResponse.builder()
                    .allowed(true)
                    .clientId(clientId)
                    .message("Request allowed")
                    .retryAfterSeconds(0)
                    .build();
        } else {
            return RateLimitResponse.builder()
                    .allowed(false)
                    .clientId(clientId)
                    .message("Rate limit exceeded. Try again later.")
                    .retryAfterSeconds(WINDOW_SIZE_SECONDS)
                    .build();
        }
    }
}