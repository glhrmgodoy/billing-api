package com.godoy.billing.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentConfirmedEvent(
        UUID paymentId,
        UUID invoiceId,
        BigDecimal amount
) {
}
