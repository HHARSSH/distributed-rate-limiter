package com.harsh.rate_limiter.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfig {

    @NotBlank(message = "clientId cannot be blank")
    private String clientId;

    @Min(value = 1, message = "maxRequests must be at least 1")
    private int maxRequests;

    @Min(value = 1, message = "windowSizeSeconds must be at least 1")
    private int windowSizeSeconds;

    @NotNull(message = "algorithm cannot be null")
    @Pattern(regexp = "TOKEN_BUCKET|SLIDING_WINDOW",
            message = "algorithm must be TOKEN_BUCKET or SLIDING_WINDOW")
    private String algorithm;
}