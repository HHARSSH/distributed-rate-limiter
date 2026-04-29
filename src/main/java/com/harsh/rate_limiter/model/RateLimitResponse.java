package com.harsh.rate_limiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {
    private boolean allowed;
    private String clientId;
    private String message;
    private long retryAfterSeconds;
    private long remainingRequests;
    private String algorithm;
}