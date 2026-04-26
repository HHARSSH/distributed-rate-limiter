package com.harsh.rate_limiter.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitResponse {
    private boolean allowed;
    private String clientId;
    private String message;
    private long retryAfterSeconds;
}