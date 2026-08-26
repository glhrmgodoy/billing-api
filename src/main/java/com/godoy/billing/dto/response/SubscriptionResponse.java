package com.godoy.billing.dto.response;

import com.godoy.billing.domain.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String planName,
        SubscriptionStatus status,
        LocalDate currentCycleStart,
        LocalDate currentCycleEnd,
        LocalDateTime createdAt
) {
}
