package com.harsh.rate_limiter.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String clientId) {
        super("Client not found: " + clientId);
    }
}