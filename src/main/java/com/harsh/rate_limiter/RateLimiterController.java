package com.harsh.rate_limiter;

import com.harsh.rate_limiter.exception.InvalidConfigException;
import com.harsh.rate_limiter.model.ClientConfig;
import com.harsh.rate_limiter.model.RateLimitResponse;
import com.harsh.rate_limiter.service.RateLimiterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    // Check if a client is allowed
    @PostMapping("/rate-limit/check/{clientId}")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@PathVariable String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new InvalidConfigException("clientId cannot be blank");
        }
        RateLimitResponse response = rateLimiterService.checkLimit(clientId);
        if (response.isAllowed()) {
            return ResponseEntity.ok(response);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Retry-After", String.valueOf(response.getRetryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(response);
        }
    }

    // Register a new client with custom config
    @PostMapping("/clients")
    public ResponseEntity<ClientConfig> registerClient(@Valid @RequestBody ClientConfig config) {
        ClientConfig saved = rateLimiterService.registerClient(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Get current config for a client
    @GetMapping("/clients/{clientId}/status")
    public ResponseEntity<ClientConfig> getClientStatus(@PathVariable String clientId) {
        ClientConfig config = rateLimiterService.getClientConfig(clientId);
        return ResponseEntity.ok(config);
    }

    // Reset a client's rate limit
    @DeleteMapping("/clients/{clientId}/reset")
    public ResponseEntity<String> resetClient(@PathVariable String clientId) {
        rateLimiterService.resetClient(clientId);
        return ResponseEntity.ok("Rate limit reset for client: " + clientId);
    }
}