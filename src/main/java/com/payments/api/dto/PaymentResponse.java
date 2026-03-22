package com.payments.api.dto;

import com.payments.model.Payment;
import com.payments.model.PaymentStatus;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentResponse {

    private UUID id;
    private UUID userId;
    private Long amount;
    private String currency;
    private PaymentStatus status;
    private Instant createdAt;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setUserId(p.getUserId());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}