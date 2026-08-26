package com.godoy.billing.dto.response;

import com.godoy.billing.domain.enums.BillingCycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String name,
        BigDecimal price,
        BillingCycle billingCycle,
        Boolean active,
        LocalDateTime createdAt
) {
}
