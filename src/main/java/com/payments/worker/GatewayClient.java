package com.payments.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub gateway client. Replace with real Stripe/Razorpay SDK calls.
 * 
 * Key contract: always pass your internal payment ID as the gateway
 * idempotency key so the same charge intent is returned on retry.
 */
@Component
@Slf4j
public class GatewayClient {

    @Value("${payments.gateway.timeout-ms}")
    private long timeoutMs;

    public GatewayResult charge(UUID paymentId, long amount, String currency) {
        log.info("Calling gateway for payment {}", paymentId);
        // Replace with: stripe.PaymentIntent.create(..., idempotencyKey = paymentId.toString())
        return new GatewayResult("gw_" + paymentId, GatewayStatus.SUCCESS, null);
    }

    public GatewayResult retrieve(String gatewayPaymentId) {
        log.info("Polling gateway for {}", gatewayPaymentId);
        // Replace with: stripe.PaymentIntent.retrieve(gatewayPaymentId)
        return new GatewayResult(gatewayPaymentId, GatewayStatus.SUCCESS, null);
    }

    public record GatewayResult(String gatewayPaymentId, GatewayStatus status, String errorCode) {}

    public enum GatewayStatus { SUCCESS, FAILED, PENDING }
}