package com.godoy.billing.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceOverdueEvent(
        UUID invoiceId,
        String customerEmail,
        String customerName,
        String planName,
        BigDecimal amount,
        LocalDate dueDate
) {
}
