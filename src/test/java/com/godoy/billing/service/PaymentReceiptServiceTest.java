package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.PaymentMessageConsumer;
import com.godoy.billing.event.PaymentConfirmedEvent;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

    @Mock
    private PaymentMessageDeduplicationService paymentMessageDeduplicationService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentReceiptService paymentReceiptService;

    private Invoice buildInvoice(Customer customer) {
        Subscription subscription = Subscription.builder().id(UUID.randomUUID()).customer(customer).build();

        return Invoice.builder().id(UUID.randomUUID()).subscription(subscription).build();
    }

    private Customer buildCustomer() {
        return Customer.builder().id(UUID.randomUUID()).name("Maria").email("maria@example.com").build();
    }

    private PaymentConfirmedEvent buildEvent(UUID invoiceId) {
        return new PaymentConfirmedEvent(UUID.randomUUID(), invoiceId, new BigDecimal("99.90"));
    }

    @Nested
    @DisplayName("sendReceipt")
    class SendReceipt {

        @Test
        @DisplayName("deve enviar o recibo para o e-mail do cliente da fatura")
        void shouldSendReceiptToInvoiceCustomer() {
            Invoice invoice = buildInvoice(buildCustomer());
            PaymentConfirmedEvent event = buildEvent(invoice.getId());

            when(paymentMessageDeduplicationService.tryMarkAsProcessed(event.paymentId(), PaymentMessageConsumer.PAYMENT_RECEIPT))
                    .thenReturn(true);
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

            paymentReceiptService.sendReceipt(event);

            ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendEmail(eq("maria@example.com"), anyString(), textCaptor.capture());

            assertThat(textCaptor.getValue()).contains("Maria", "99,90");
        }

        @Test
        @DisplayName("deve ignorar a mensagem duplicada sem buscar a fatura nem enviar e-mail")
        void shouldIgnoreDuplicateMessage() {
            PaymentConfirmedEvent event = buildEvent(UUID.randomUUID());

            when(paymentMessageDeduplicationService.tryMarkAsProcessed(event.paymentId(), PaymentMessageConsumer.PAYMENT_RECEIPT))
                    .thenReturn(false);

            paymentReceiptService.sendReceipt(event);

            verifyNoInteractions(invoiceRepository, emailService);
        }

        @Test
        @DisplayName("deve lançar NotFoundException sem enviar e-mail quando a fatura não existir")
        void shouldThrowExceptionWhenInvoiceDoesNotExist() {
            PaymentConfirmedEvent event = buildEvent(UUID.randomUUID());

            when(paymentMessageDeduplicationService.tryMarkAsProcessed(event.paymentId(), PaymentMessageConsumer.PAYMENT_RECEIPT))
                    .thenReturn(true);
            when(invoiceRepository.findById(event.invoiceId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentReceiptService.sendReceipt(event))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("deve propagar a falha do envio de e-mail para que a mensagem seja reentregue")
        void shouldPropagateMailFailure() {
            Invoice invoice = buildInvoice(buildCustomer());
            PaymentConfirmedEvent event = buildEvent(invoice.getId());

            when(paymentMessageDeduplicationService.tryMarkAsProcessed(event.paymentId(), PaymentMessageConsumer.PAYMENT_RECEIPT))
                    .thenReturn(true);
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
            doThrow(new MailSendException("SES indisponível"))
                    .when(emailService).sendEmail(anyString(), anyString(), anyString());

            assertThatThrownBy(() -> paymentReceiptService.sendReceipt(event))
                    .isInstanceOf(MailSendException.class);
        }
    }
}
