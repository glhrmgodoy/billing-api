package com.godoy.billing.service;

import com.godoy.billing.domain.entity.ProcessedPaymentMessage;
import com.godoy.billing.domain.entity.ProcessedPaymentMessageId;
import com.godoy.billing.domain.enums.PaymentMessageConsumer;
import com.godoy.billing.repository.ProcessedPaymentMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentMessageDeduplicationService {

    private final ProcessedPaymentMessageRepository processedPaymentMessageRepository;

    @Transactional
    public boolean tryMarkAsProcessed(UUID paymentId, PaymentMessageConsumer consumer) {
        ProcessedPaymentMessageId processedPaymentMessageId = new ProcessedPaymentMessageId(paymentId, consumer);

        if (processedPaymentMessageRepository.existsById(processedPaymentMessageId)) {
            log.info("Duplicata ignorada: {}", processedPaymentMessageId);

            return false;
        }

        try {
            processedPaymentMessageRepository.saveAndFlush(
                    ProcessedPaymentMessage.builder()
                            .id(processedPaymentMessageId)
                            .build()
            );

            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Constraint de duplicidade barrou insert concorrente para mensagem: {}", processedPaymentMessageId);

            return false;
        }
    }
}
