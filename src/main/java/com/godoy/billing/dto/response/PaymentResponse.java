package com.godoy.billing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        BigDecimal amountPaid,
        LocalDateTime paidAt,
        String idempotencyKey
) {
}
