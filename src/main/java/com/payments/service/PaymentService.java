package com.payments.service;

import com.payments.api.dto.CreatePaymentRequest;
import com.payments.api.dto.PaymentResponse;
import com.payments.model.Payment;
import com.payments.model.PaymentStatus;
import com.payments.repository.PaymentRepository;
import com.payments.worker.PaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentMessage> kafkaTemplate;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        Payment payment = Payment.builder()
            .userId(request.getUserId())
            .idempotencyKey(idempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.PENDING)
            .requestHash(hashRequest(request))
            .build();

        try {
            // DB write BEFORE queue publish — if queue fails, reconciliation catches stuck PENDING
            Payment saved = paymentRepository.save(payment);
            kafkaTemplate.send("payments.process", saved.getId().toString(),
                new PaymentMessage(saved.getId()));
            log.info("Payment created: {}", saved.getId());
            return PaymentResponse.from(saved);

        } catch (DataIntegrityViolationException e) {
            // Unique constraint hit — Redis was down but Postgres caught the duplicate
            log.warn("Duplicate idempotency key detected for user {} key {}", 
                request.getUserId(), idempotencyKey);
            return paymentRepository
                .findByUserIdAndIdempotencyKey(request.getUserId(), idempotencyKey)
                .map(PaymentResponse::from)
                .orElseThrow();
        }
    }

    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id);
    }

    private String hashRequest(CreatePaymentRequest request) {
        try {
            String canonical = String.format("%s:%d:%s",
                request.getUserId(), request.getAmount(), request.getCurrency());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }
}