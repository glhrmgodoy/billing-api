package com.godoy.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        @NotNull(message = "A fatura é obrigatória")
        UUID invoiceId,

        @NotNull(message = "O valor pago é obrigatório")
        @Positive(message = "O valor pago deve ser maior que zero")
        BigDecimal amountPaid,

        @NotBlank(message = "A chave de idempotência é obrigatória")
        String idempotencyKey
) {
}
