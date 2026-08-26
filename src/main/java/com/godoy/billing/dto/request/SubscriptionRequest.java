package com.godoy.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscriptionRequest(
        @NotNull(message = "O plano é obrigatório")
        UUID planId
) {
}
