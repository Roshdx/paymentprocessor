package com.payments.model;

import com.payments.exception.InvalidTransitionException;
import java.util.Map;
import java.util.Set;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    UNKNOWN,
    REFUNDED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.of(
        PENDING,    Set.of(PROCESSING),
        PROCESSING, Set.of(SUCCESS, FAILED, UNKNOWN),
        UNKNOWN,    Set.of(SUCCESS, FAILED),
        SUCCESS,    Set.of(REFUNDED),
        FAILED,     Set.of(),
        REFUNDED,   Set.of()
    );

    public void validateTransitionTo(PaymentStatus next) {
        if (!VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(next)) {
            throw new InvalidTransitionException(
                String.format("Invalid transition: %s -> %s", this, next)
            );
        }
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == REFUNDED;
    }
}