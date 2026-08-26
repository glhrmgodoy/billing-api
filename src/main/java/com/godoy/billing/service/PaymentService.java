package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.entity.Payment;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.InvoiceStatus;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import com.godoy.billing.dto.request.PaymentRequest;
import com.godoy.billing.dto.response.PaymentResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.PaymentMapper;
import com.godoy.billing.repository.InvoiceRepository;
import com.godoy.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse confirmPayment(PaymentRequest request) {
        Optional<Payment> alreadyProcessed = paymentRepository.findByIdempotencyKey(request.idempotencyKey());

        if (alreadyProcessed.isPresent()) {
            log.info("Webhook duplicado ignorado (chave já processada): {}", request.idempotencyKey());
            return paymentMapper.toResponse(alreadyProcessed.get());
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> NotFoundException.of("Fatura", request.invoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Esta fatura já foi paga");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amountPaid(request.amountPaid())
                .paidAt(LocalDateTime.now())
                .idempotencyKey(request.idempotencyKey())
                .build();

        Payment saved;

        try {
            saved = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            log.info("Constraint de idempotência barrou insert concorrente: {}", request.idempotencyKey());

            return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                    .map(paymentMapper::toResponse)
                    .orElseThrow(() -> e);
        }

        invoice.setStatus(InvoiceStatus.PAID);

        Subscription subscription = invoice.getSubscription();

        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        return paymentMapper.toResponse(saved);
    }
}
