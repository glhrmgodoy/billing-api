package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.entity.Payment;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.InvoiceStatus;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import com.godoy.billing.dto.request.PaymentWebhookRequest;
import com.godoy.billing.dto.response.PaymentResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.PaymentMapper;
import com.godoy.billing.repository.InvoiceRepository;
import com.godoy.billing.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Subscription buildSubscription(SubscriptionStatus status) {
        return Subscription.builder().id(UUID.randomUUID()).status(status).build();
    }

    private Invoice buildInvoice(InvoiceStatus status, Subscription subscription) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .subscription(subscription)
                .amount(BigDecimal.valueOf(29.90))
                .status(status)
                .version(0L)
                .build();
    }

    private PaymentWebhookRequest buildRequest(UUID invoiceId, String idempotencyKey) {
        return new PaymentWebhookRequest(invoiceId, BigDecimal.valueOf(29.90), idempotencyKey);
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("should confirm the payment and mark the invoice as PAID")
        void shouldConfirmPaymentSuccessfully() {
            Subscription subscription = buildSubscription(SubscriptionStatus.ACTIVE);
            Invoice invoice = buildInvoice(InvoiceStatus.PENDING, subscription);
            PaymentWebhookRequest request = buildRequest(invoice.getId(), "evt_123");
            Payment savedPayment = Payment.builder().id(UUID.randomUUID()).invoice(invoice)
                    .amountPaid(request.amountPaid()).paidAt(LocalDateTime.now())
                    .idempotencyKey(request.idempotencyKey()).build();
            PaymentResponse response = new PaymentResponse(
                    savedPayment.getId(), invoice.getId(), request.amountPaid(), LocalDateTime.now(), "evt_123");

            when(paymentRepository.findByIdempotencyKey("evt_123")).thenReturn(Optional.empty());
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
            when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);
            when(paymentMapper.toResponse(savedPayment)).thenReturn(response);

            PaymentResponse result = paymentService.confirmPayment(request);

            assertEquals(response, result);
            assertEquals(InvoiceStatus.PAID, invoice.getStatus());
            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        }

        @Test
        @DisplayName("should reactivate a suspended subscription when the payment is confirmed")
        void shouldReactivateSuspendedSubscription() {
            Subscription subscription = buildSubscription(SubscriptionStatus.SUSPENDED);
            Invoice invoice = buildInvoice(InvoiceStatus.OVERDUE, subscription);
            PaymentWebhookRequest request = buildRequest(invoice.getId(), "evt_456");
            Payment savedPayment = Payment.builder().id(UUID.randomUUID()).invoice(invoice)
                    .amountPaid(request.amountPaid()).paidAt(LocalDateTime.now())
                    .idempotencyKey(request.idempotencyKey()).build();

            when(paymentRepository.findByIdempotencyKey("evt_456")).thenReturn(Optional.empty());
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
            when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);
            when(paymentMapper.toResponse(savedPayment)).thenReturn(
                    new PaymentResponse(savedPayment.getId(), invoice.getId(), request.amountPaid(), LocalDateTime.now(), "evt_456"));

            paymentService.confirmPayment(request);

            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        }

        @Test
        @DisplayName("should return the already-processed payment without trying to save it again (idempotency)")
        void shouldReturnAlreadyProcessedPayment() {
            Invoice invoice = buildInvoice(InvoiceStatus.PAID, buildSubscription(SubscriptionStatus.ACTIVE));
            Payment existingPayment = Payment.builder().id(UUID.randomUUID()).invoice(invoice)
                    .amountPaid(BigDecimal.valueOf(29.90)).paidAt(LocalDateTime.now())
                    .idempotencyKey("evt_duplicate").build();
            PaymentResponse response = new PaymentResponse(
                    existingPayment.getId(), invoice.getId(), existingPayment.getAmountPaid(),
                    existingPayment.getPaidAt(), "evt_duplicate");
            PaymentWebhookRequest request = buildRequest(invoice.getId(), "evt_duplicate");

            when(paymentRepository.findByIdempotencyKey("evt_duplicate")).thenReturn(Optional.of(existingPayment));
            when(paymentMapper.toResponse(existingPayment)).thenReturn(response);

            PaymentResponse result = paymentService.confirmPayment(request);

            assertEquals(response, result);
            verifyNoInteractions(invoiceRepository);
            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when the invoice does not exist")
        void shouldThrowExceptionWhenInvoiceDoesNotExist() {
            UUID invoiceId = UUID.randomUUID();
            PaymentWebhookRequest request = buildRequest(invoiceId, "evt_789");

            when(paymentRepository.findByIdempotencyKey("evt_789")).thenReturn(Optional.empty());
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> paymentService.confirmPayment(request));
            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("should throw BusinessException when the invoice is already paid")
        void shouldThrowExceptionWhenInvoiceAlreadyPaid() {
            Invoice invoice = buildInvoice(InvoiceStatus.PAID, buildSubscription(SubscriptionStatus.ACTIVE));
            PaymentWebhookRequest request = buildRequest(invoice.getId(), "evt_new_key");

            when(paymentRepository.findByIdempotencyKey("evt_new_key")).thenReturn(Optional.empty());
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

            assertThrows(BusinessException.class, () -> paymentService.confirmPayment(request));
            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("should recover the concurrently-inserted payment when the idempotency constraint blocks the insert")
        void shouldRecoverPaymentAfterConcurrencyConflict() {
            Subscription subscription = buildSubscription(SubscriptionStatus.ACTIVE);
            Invoice invoice = buildInvoice(InvoiceStatus.PENDING, subscription);
            PaymentWebhookRequest request = buildRequest(invoice.getId(), "evt_concurrent");

            Payment paymentFromOtherThread = Payment.builder().id(UUID.randomUUID()).invoice(invoice)
                    .amountPaid(request.amountPaid()).paidAt(LocalDateTime.now())
                    .idempotencyKey("evt_concurrent").build();
            PaymentResponse response = new PaymentResponse(
                    paymentFromOtherThread.getId(), invoice.getId(), request.amountPaid(), LocalDateTime.now(), "evt_concurrent");

            when(paymentRepository.findByIdempotencyKey("evt_concurrent"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(paymentFromOtherThread));
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
            when(paymentRepository.saveAndFlush(any(Payment.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));
            when(paymentMapper.toResponse(paymentFromOtherThread)).thenReturn(response);

            PaymentResponse result = paymentService.confirmPayment(request);

            assertEquals(response, result);
            assertEquals(InvoiceStatus.PENDING, invoice.getStatus());
        }
    }
}