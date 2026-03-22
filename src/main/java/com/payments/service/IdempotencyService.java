package com.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.api.dto.PaymentResponse;
import com.payments.exception.IdempotencyKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${payments.idempotency.ttl-seconds}")
    private long ttlSeconds;

    // Key format: idempotency:{userId}:{clientKey}
    // Namespaced by userId to prevent cross-user collisions
    private String redisKey(UUID userId, String clientKey) {
        return String.format("idempotency:%s:%s", userId, clientKey);
    }

    public void validateKeyFormat(String key) {
        if (key == null || key.isBlank()) {
            throw new IdempotencyKeyException("Idempotency-Key header is required");
        }
        if (key.length() > 255) {
            throw new IdempotencyKeyException("Idempotency-Key must be under 255 characters");
        }
        try {
            UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new IdempotencyKeyException("Idempotency-Key must be a valid UUID v4");
        }
    }

    public Optional<PaymentResponse> getCachedResponse(UUID userId, String key) {
        String stored = redis.opsForValue().get(redisKey(userId, key));
        if (stored == null) return Optional.empty();

        try {
            // Reset TTL on read — active clients keep the window alive
            redis.expire(redisKey(userId, key), ttlSeconds, TimeUnit.SECONDS);
            return Optional.of(objectMapper.readValue(stored, PaymentResponse.class));
        } catch (Exception e) {
            log.error("Failed to deserialize cached idempotency response for key {}", key, e);
            return Optional.empty();
        }
    }

    public void cacheResponse(UUID userId, String key, PaymentResponse response) {
        try {
            String value = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(redisKey(userId, key), value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Non-fatal: Postgres unique constraint is the safety net
            log.warn("Failed to cache idempotency response for key {}. Postgres constraint will protect.", key, e);
        }
    }
}