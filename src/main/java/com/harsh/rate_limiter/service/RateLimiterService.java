package com.harsh.rate_limiter.service;

import com.harsh.rate_limiter.algorithm.RateLimitAlgorithm;
import com.harsh.rate_limiter.algorithm.SlidingWindowAlgorithm;
import com.harsh.rate_limiter.algorithm.TokenBucketAlgorithm;
import com.harsh.rate_limiter.exception.ClientNotFoundException;
import com.harsh.rate_limiter.exception.InvalidConfigException;
import com.harsh.rate_limiter.model.ClientConfig;
import com.harsh.rate_limiter.model.RateLimitResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final TokenBucketAlgorithm tokenBucketAlgorithm;
    private final SlidingWindowAlgorithm slidingWindowAlgorithm;
    private final StringRedisTemplate redisTemplate;

    private static final int DEFAULT_MAX_REQUESTS = 5;
    private static final int DEFAULT_WINDOW_SIZE_SECONDS = 60;
    private static final String DEFAULT_ALGORITHM = "TOKEN_BUCKET";

    public RateLimiterService(TokenBucketAlgorithm tokenBucketAlgorithm,
                              SlidingWindowAlgorithm slidingWindowAlgorithm,
                              StringRedisTemplate redisTemplate) {
        this.tokenBucketAlgorithm = tokenBucketAlgorithm;
        this.slidingWindowAlgorithm = slidingWindowAlgorithm;
        this.redisTemplate = redisTemplate;
    }

    public RateLimitResponse checkLimit(String clientId) {
        ClientConfig config = getClientConfig(clientId);

        RateLimitAlgorithm algorithm = config.getAlgorithm().equals("SLIDING_WINDOW")
                ? slidingWindowAlgorithm
                : tokenBucketAlgorithm;

        boolean allowed = algorithm.isAllowed(clientId, config.getMaxRequests(), config.getWindowSizeSeconds());

        if (allowed) {
            return RateLimitResponse.builder()
                    .allowed(true)
                    .clientId(clientId)
                    .message("Request allowed")
                    .retryAfterSeconds(0)
                    .algorithm(config.getAlgorithm())
                    .build();
        } else {
            return RateLimitResponse.builder()
                    .allowed(false)
                    .clientId(clientId)
                    .message("Rate limit exceeded. Try again later.")
                    .retryAfterSeconds(config.getWindowSizeSeconds())
                    .algorithm(config.getAlgorithm())
                    .build();
        }
    }

    public ClientConfig registerClient(ClientConfig config) {
        // Validate config
        if (config.getMaxRequests() <= 0) {
            throw new InvalidConfigException("maxRequests must be greater than 0");
        }
        if (config.getWindowSizeSeconds() <= 0) {
            throw new InvalidConfigException("windowSizeSeconds must be greater than 0");
        }
        if (!config.getAlgorithm().equals("TOKEN_BUCKET") &&
                !config.getAlgorithm().equals("SLIDING_WINDOW")) {
            throw new InvalidConfigException("algorithm must be TOKEN_BUCKET or SLIDING_WINDOW");
        }

        String key = "client_config:" + config.getClientId();
        redisTemplate.opsForHash().put(key, "maxRequests", String.valueOf(config.getMaxRequests()));
        redisTemplate.opsForHash().put(key, "windowSizeSeconds", String.valueOf(config.getWindowSizeSeconds()));
        redisTemplate.opsForHash().put(key, "algorithm", config.getAlgorithm());
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        return config;
    }

    public ClientConfig getClientConfig(String clientId) {
        String key = "client_config:" + clientId;
        String maxRequests = (String) redisTemplate.opsForHash().get(key, "maxRequests");
        String windowSizeSeconds = (String) redisTemplate.opsForHash().get(key, "windowSizeSeconds");
        String algorithm = (String) redisTemplate.opsForHash().get(key, "algorithm");

        if (maxRequests == null) {
            // Return defaults for unregistered clients
            return ClientConfig.builder()
                    .clientId(clientId)
                    .maxRequests(DEFAULT_MAX_REQUESTS)
                    .windowSizeSeconds(DEFAULT_WINDOW_SIZE_SECONDS)
                    .algorithm(DEFAULT_ALGORITHM)
                    .build();
        }

        return ClientConfig.builder()
                .clientId(clientId)
                .maxRequests(Integer.parseInt(maxRequests))
                .windowSizeSeconds(Integer.parseInt(windowSizeSeconds))
                .algorithm(algorithm)
                .build();
    }

    public void resetClient(String clientId) {
        String key = "client_config:" + clientId;
        // Check if client exists
        String maxRequests = (String) redisTemplate.opsForHash().get(key, "maxRequests");
        if (maxRequests == null) {
            throw new ClientNotFoundException(clientId);
        }
        redisTemplate.delete("token_bucket:tokens:" + clientId);
        redisTemplate.delete("token_bucket:timestamp:" + clientId);
        redisTemplate.delete("sliding_window:" + clientId);
    }
}