package com.payments.api;

import com.payments.api.dto.CreatePaymentRequest;
import com.payments.api.dto.PaymentResponse;
import com.payments.service.IdempotencyService;
import com.payments.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        idempotencyService.validateKeyFormat(idempotencyKey);

        // Check cache first — return stored response if key already seen
        return idempotencyService.getCachedResponse(request.getUserId(), idempotencyKey)
            .map(ResponseEntity::ok)
            .orElseGet(() -> {
                PaymentResponse response = paymentService.createPayment(
                    request, idempotencyKey
                );
                idempotencyService.cacheResponse(
                    request.getUserId(), idempotencyKey, response
                );
                return ResponseEntity.status(201).body(response);
            });
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return paymentService.findById(id)
            .map(PaymentResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}