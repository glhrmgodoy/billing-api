package com.godoy.billing.dto.request;

import com.godoy.billing.domain.enums.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "O nome do plano é obrigatório")
        String name,

        @NotNull(message = "O preço é obrogatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal price,

        @NotNull(message = "A periodicidade é obrigatória")
        BillingCycle billingCycle
) {
}
