package com.godoy.billing.event;

import java.util.UUID;

public record SubscriptionConfirmedEvent(
        UUID subscriptionId,
        String customerEmail,
        String customerName,
        String planName
) {
}
