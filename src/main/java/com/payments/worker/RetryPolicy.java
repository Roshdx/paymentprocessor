package com.payments.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RetryPolicy {

    @Value("${payments.retry.max-attempts}")
    private int maxAttempts;

    @Value("${payments.retry.base-delay-seconds}")
    private long baseDelaySeconds;

    private final Random random = new Random();

    public boolean hasAttemptsLeft(int attempt) {
        return attempt < maxAttempts;
    }

    // Exponential backoff with jitter — prevents thundering herd on mass failure
    public long delayMs(int attempt) {
        long delay = (long) Math.pow(baseDelaySeconds, attempt) * 1000;
        long jitter = (long) (delay * 0.2 * random.nextDouble());
        return delay + jitter;
    }

    public static final java.util.Set<String> NON_RETRYABLE_ERRORS = java.util.Set.of(
        "insufficient_funds",
        "card_declined",
        "invalid_card_number",
        "do_not_honor",
        "card_expired"
    );

    public boolean isRetryable(String errorCode) {
        return !NON_RETRYABLE_ERRORS.contains(errorCode);
    }
}