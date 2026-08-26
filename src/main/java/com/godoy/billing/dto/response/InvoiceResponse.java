package com.godoy.billing.dto.response;

import com.godoy.billing.domain.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID subscriptionId,
        BigDecimal amount,
        LocalDate dueDate,
        InvoiceStatus status,
        LocalDateTime createdAt
) {
}
