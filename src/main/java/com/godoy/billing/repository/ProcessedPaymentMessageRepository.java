package com.godoy.billing.repository;

import com.godoy.billing.domain.entity.ProcessedPaymentMessage;
import com.godoy.billing.domain.entity.ProcessedPaymentMessageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentMessageRepository extends JpaRepository<ProcessedPaymentMessage, ProcessedPaymentMessageId> {
}
