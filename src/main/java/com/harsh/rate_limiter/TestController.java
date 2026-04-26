package com.harsh.rate_limiter;

import com.harsh.rate_limiter.model.RateLimitResponse;
import com.harsh.rate_limiter.service.RateLimiterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {

    private final RateLimiterService rateLimiterService;

    public TestController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/check/{clientId}")
    public RateLimitResponse checkRate(@PathVariable String clientId) {
        return rateLimiterService.checkLimit(clientId);
    }
}