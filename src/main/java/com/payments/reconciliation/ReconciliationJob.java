package com.payments.reconciliation;

import com.payments.model.Payment;
import com.payments.model.PaymentStatus;
import com.payments.repository.PaymentRepository;
import com.payments.worker.GatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final GatewayClient gatewayClient;

    @Value("${payments.reconciliation.unknown-payment-max-age-hours}")
    private int maxAgeHours;

    // Resolve UNKNOWN payments by polling the gateway
    @Scheduled(cron = "${payments.reconciliation.cron}")
    public void resolveUnknownPayments() {
        log.info("Reconciliation job started");

        List<Payment> unknowns = paymentRepository
            .findByStatus(PaymentStatus.UNKNOWN);

        int resolved = 0, escalated = 0;

        for (Payment payment : unknowns) {
            if (payment.getGatewayPaymentId() == null) {
                log.warn("UNKNOWN payment {} has no gateway ID — escalating", payment.getId());
                escalated++;
                continue;
            }

            GatewayClient.GatewayResult result =
                gatewayClient.retrieve(payment.getGatewayPaymentId());

            boolean tooOld = payment.getCreatedAt()
                .isBefore(Instant.now().minus(maxAgeHours, ChronoUnit.HOURS));

            switch (result.status()) {
                case SUCCESS -> {
                    payment.transitionTo(PaymentStatus.SUCCESS);
                    paymentRepository.save(payment);
                    resolved++;
                }
                case FAILED -> {
                    payment.transitionTo(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    resolved++;
                }
                case PENDING -> {
                    if (tooOld) {
                        // Never auto-fail — escalate to manual review
                        log.error("ESCALATE: Payment {} is UNKNOWN after {}h — manual review required",
                            payment.getId(), maxAgeHours);
                        escalated++;
                    }
                }
            }
        }

        log.info("Reconciliation complete. Resolved: {}, Escalated: {}, Total UNKNOWN: {}",
            resolved, escalated, unknowns.size());
    }
}