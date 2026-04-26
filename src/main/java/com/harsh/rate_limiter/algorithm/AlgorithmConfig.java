package com.harsh.rate_limiter.algorithm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AlgorithmConfig {

    @Bean
    @Primary
    public RateLimitAlgorithm rateLimitAlgorithm(StringRedisTemplate redisTemplate) {
        return new TokenBucketAlgorithm (redisTemplate);
    }
}