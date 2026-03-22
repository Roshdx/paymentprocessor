package com.payments.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class CreatePaymentRequest {

    @NotNull
    private UUID userId;

    @NotNull @Min(1)
    private Long amount;       // in paise/cents

    @NotBlank @Size(min = 3, max = 3)
    private String currency;
}