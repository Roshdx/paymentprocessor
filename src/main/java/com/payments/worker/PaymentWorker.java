package com.payments.worker;

import com.payments.model.Payment;
import com.payments.model.PaymentEvent;
import com.payments.model.PaymentStatus;
import com.payments.repository.PaymentEventRepository;
import com.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWorker {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final GatewayClient gatewayClient;
    private final RetryPolicy retryPolicy;
    private final KafkaTemplate<String, PaymentMessage> kafkaTemplate;

    @KafkaListener(topics = "payments.process", groupId = "payment-processor")
    @Transactional
    public void process(PaymentMessage message) {
        Payment payment = paymentRepository
            .findByIdForUpdate(message.getPaymentId())   // SELECT FOR UPDATE SKIP LOCKED
            .orElseGet(() -> {
                log.warn("Payment {} not found", message.getPaymentId());
                return null;
            });

        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) return;

        transition(payment, PaymentStatus.PROCESSING, "Worker picked up");

        try {
            GatewayClient.GatewayResult result = gatewayClient.charge(
                payment.getId(), payment.getAmount(), payment.getCurrency()
            );

            // Always store gateway ID before checking status — needed for UNKNOWN polling
            payment.setGatewayPaymentId(result.gatewayPaymentId());

            switch (result.status()) {
                case SUCCESS -> {
                    payment.setGatewayResponse(Map.of("status", "succeeded", "id", result.gatewayPaymentId()));
                    transition(payment, PaymentStatus.SUCCESS, "Gateway confirmed");
                }
                case FAILED -> {
                    if (retryPolicy.isRetryable(result.errorCode())) {
                        handleRetry(payment, result.errorCode(), 1);
                    } else {
                        transition(payment, PaymentStatus.FAILED, "Non-retryable: " + result.errorCode());
                    }
                }
                case PENDING -> transition(payment, PaymentStatus.UNKNOWN, "Gateway timeout");
            }

        } catch (Exception e) {
            // Treat unexpected exception as timeout — never auto-fail
            log.error("Unexpected error processing payment {}", payment.getId(), e);
            transition(payment, PaymentStatus.UNKNOWN, "Worker exception: " + e.getMessage());
        }

        paymentRepository.save(payment);
    }

    private void handleRetry(Payment payment, String errorCode, int attempt) {
        if (retryPolicy.hasAttemptsLeft(attempt)) {
            log.info("Scheduling retry {} for payment {}", attempt, payment.getId());
            kafkaTemplate.send("payments.process", payment.getId().toString(),
                new PaymentMessage(payment.getId()));
        } else {
            transition(payment, PaymentStatus.FAILED, "Max retries exhausted");
            kafkaTemplate.send("payments.dead-letter", payment.getId().toString(),
                new PaymentMessage(payment.getId()));
        }
    }

    private void transition(Payment payment, PaymentStatus next, String reason) {
        PaymentStatus from = payment.getStatus();
        payment.transitionTo(next);
        paymentEventRepository.save(PaymentEvent.builder()
            .paymentId(payment.getId())
            .fromStatus(from)
            .toStatus(next)
            .reason(reason)
            .build());
        log.info("Payment {} transitioned {} -> {}", payment.getId(), from, next);
    }
}