package com.godoy.billing.domain.entity;

import com.godoy.billing.domain.enums.PaymentMessageConsumer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record ProcessedPaymentMessageId(

        @Column(name = "payment_id", nullable = false)
        UUID paymentId,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 40)
        PaymentMessageConsumer consumer
) implements Serializable {
}
