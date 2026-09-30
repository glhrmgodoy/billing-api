package com.godoy.billing.service;

import com.godoy.billing.domain.entity.ProcessedPaymentMessage;
import com.godoy.billing.domain.entity.ProcessedPaymentMessageId;
import com.godoy.billing.domain.enums.PaymentMessageConsumer;
import com.godoy.billing.repository.ProcessedPaymentMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMessageDeduplicationServiceTest {

    @Mock
    private ProcessedPaymentMessageRepository processedPaymentMessageRepository;

    @InjectMocks
    private PaymentMessageDeduplicationService paymentMessageDeduplicationService;

    private final UUID paymentId = UUID.randomUUID();

    private final PaymentMessageConsumer consumer = PaymentMessageConsumer.PAYMENT_RECEIPT;

    private final ProcessedPaymentMessageId expectedId = new ProcessedPaymentMessageId(paymentId, consumer);

    @Nested
    @DisplayName("tryMarkAsProcessed")
    class TryMarkAsProcessed {

        @Test
        @DisplayName("deve gravar o marcador com paymentId e consumer e retornar true quando a mensagem for nova")
        void shouldMarkNewMessageAsProcessed() {
            when(processedPaymentMessageRepository.existsById(expectedId)).thenReturn(false);

            boolean result = paymentMessageDeduplicationService.tryMarkAsProcessed(paymentId, consumer);

            ArgumentCaptor<ProcessedPaymentMessage> captor = ArgumentCaptor.forClass(ProcessedPaymentMessage.class);
            verify(processedPaymentMessageRepository).saveAndFlush(captor.capture());

            assertThat(result).isTrue();
            assertThat(captor.getValue().getId()).isEqualTo(expectedId);
        }

        @Test
        @DisplayName("deve retornar false sem gravar quando a mensagem já tiver sido processada por esse consumidor")
        void shouldIgnoreDuplicateMessage() {
            when(processedPaymentMessageRepository.existsById(expectedId)).thenReturn(true);

            boolean result = paymentMessageDeduplicationService.tryMarkAsProcessed(paymentId, consumer);

            assertThat(result).isFalse();
            verify(processedPaymentMessageRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("deve retornar false sem propagar a exceção quando a constraint barrar um insert concorrente")
        void shouldReturnFalseWhenConcurrentInsertViolatesConstraint() {
            when(processedPaymentMessageRepository.existsById(expectedId)).thenReturn(false);
            when(processedPaymentMessageRepository.saveAndFlush(any(ProcessedPaymentMessage.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            boolean result = paymentMessageDeduplicationService.tryMarkAsProcessed(paymentId, consumer);

            assertThat(result).isFalse();
        }
    }
}
