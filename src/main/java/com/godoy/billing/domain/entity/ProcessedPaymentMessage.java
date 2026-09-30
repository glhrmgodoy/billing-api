package com.godoy.billing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_payment_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedPaymentMessage {

    @EmbeddedId
    private ProcessedPaymentMessageId id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
