# Distributed Rate Limiter Service

A production-inspired distributed rate limiting microservice built with Java, Spring Boot, and Redis. Supports two algorithms and per-client configuration — consumable by any backend service via REST.

## What it does

- Limits the number of requests a client can make in a given time window
- Supports two algorithms: **Token Bucket** and **Sliding Window**
- Per-client configuration stored in Redis — different limits for different clients
- Unregistered clients automatically get default limits
- Returns proper HTTP 429 status with `Retry-After` header when rate limited

## Tech Stack

- **Java 21**
- **Spring Boot 3.5**
- **Redis 7** (via Docker)
- **Maven**
- **JUnit 5 + Mockito** (testing)
- **Docker Compose**

## Architecture

Client Request
↓
RateLimiterController (REST Layer)
↓
RateLimiterService (Business Logic)
↓
RateLimitAlgorithm (Interface)
↙        ↘
Token      Sliding
Bucket     Window
↓          ↓
Redis

## Algorithms

### Token Bucket
- Each client has a bucket with max N tokens
- Each request consumes 1 token
- Tokens refill at a fixed rate over time
- Allows short bursts — client can use all tokens instantly

### Sliding Window
- Tracks timestamps of all requests in a Redis Sorted Set
- On each request, removes timestamps older than the window
- Counts remaining entries — if at limit, rejects
- More precise — no burst allowance

## API Endpoints

### Check Rate Limit

POST /api/rate-limit/check/{clientId}

Returns `200 OK` if allowed, `429 Too Many Requests` if rate limited.

Response:
```json
{
    "allowed": true,
    "clientId": "client1",
    "message": "Request allowed",
    "retryAfterSeconds": 0,
    "algorithm": "TOKEN_BUCKET"
}
```

### Register Client
POST /api/clients
Body:
```json
{
    "clientId": "client1",
    "maxRequests": 10,
    "windowSizeSeconds": 60,
    "algorithm": "TOKEN_BUCKET"
}
```

### Get Client Status
GET /api/clients/{clientId}/status

### Reset Client
DELETE /api/clients/{clientId}/reset

## How to Run

### Prerequisites
- Docker Desktop
- Java 21
- Maven

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/HHARSSH/distributed-rate-limiter.git
cd distributed-rate-limiter
```

**2. Start Redis**
```bash
docker-compose up -d
```

**3. Run the application**
```bash
./mvnw spring-boot:run
```

**4. Test the API**
```bash
# Check rate limit
curl -X POST http://localhost:8080/api/rate-limit/check/client1

# Register client with custom limits
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{"clientId":"client1","maxRequests":10,"windowSizeSeconds":60,"algorithm":"SLIDING_WINDOW"}'
```

## Known Limitations

- **Non-atomic operations**: The current Redis read-modify-write pattern is not atomic and may allow slightly more requests than configured under high concurrency. In production, this would be solved using Redis Lua scripts for atomic operations.

## Testing

```bash
./mvnw test
```

Tests include:
- Unit tests for Token Bucket Algorithm
- Unit tests for Sliding Window Algorithm
- Concurrency test proving behaviour under simultaneous requests

## Default Limits

Unregistered clients automatically get:
- Max requests: **5**
- Window size: **60 seconds**
- Algorithm: **Token Bucket**