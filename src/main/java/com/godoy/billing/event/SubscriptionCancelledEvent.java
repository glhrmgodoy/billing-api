package com.godoy.billing.event;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCancelledEvent(
        UUID subscriptionId,
        String customerEmail,
        String customerName,
        String planName,
        LocalDate cycleEnd
) {
}
