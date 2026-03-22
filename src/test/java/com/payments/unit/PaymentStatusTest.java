package com.payments.unit;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payments.exception.InvalidTransitionException;
import com.payments.model.PaymentStatus;

class PaymentStatusTest {

    @Test
    void validTransition_pendingToProcessing() {
        assertThatNoException().isThrownBy(() ->
            PaymentStatus.PENDING.validateTransitionTo(PaymentStatus.PROCESSING)
        );
    }

    @Test
    void invalidTransition_failedToProcessing_throws() {
        assertThatThrownBy(() ->
            PaymentStatus.FAILED.validateTransitionTo(PaymentStatus.PROCESSING)
        )
        .isInstanceOf(InvalidTransitionException.class)
        .hasMessageContaining("FAILED -> PROCESSING");
    }

    @Test
    void invalidTransition_successToProcessing_throws() {
        assertThatThrownBy(() ->
            PaymentStatus.SUCCESS.validateTransitionTo(PaymentStatus.PROCESSING)
        )
        .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void terminalStates_areCorrect() {
        assertThat(PaymentStatus.SUCCESS.isTerminal()).isTrue();
        assertThat(PaymentStatus.FAILED.isTerminal()).isTrue();
        assertThat(PaymentStatus.REFUNDED.isTerminal()).isTrue();
        assertThat(PaymentStatus.PENDING.isTerminal()).isFalse();
        assertThat(PaymentStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(PaymentStatus.UNKNOWN.isTerminal()).isFalse();
    }

    @Test
    void unknownCanResolveToSuccessOrFailed() {
        assertThatNoException().isThrownBy(() ->
            PaymentStatus.UNKNOWN.validateTransitionTo(PaymentStatus.SUCCESS)
        );
        assertThatNoException().isThrownBy(() ->
            PaymentStatus.UNKNOWN.validateTransitionTo(PaymentStatus.FAILED)
        );
    }
}