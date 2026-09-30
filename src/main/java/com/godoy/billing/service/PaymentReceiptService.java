package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.enums.PaymentMessageConsumer;
import com.godoy.billing.event.PaymentConfirmedEvent;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentReceiptService {

    private final PaymentMessageDeduplicationService paymentMessageDeduplicationService;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;

    @Transactional
    public void sendReceipt(PaymentConfirmedEvent event) {
        if (!paymentMessageDeduplicationService.tryMarkAsProcessed(event.paymentId(), PaymentMessageConsumer.PAYMENT_RECEIPT)) {
            return;
        }

        Invoice invoice = invoiceRepository.findById(event.invoiceId())
                .orElseThrow(() -> NotFoundException.of("Fatura", event.invoiceId()));

        Customer customer = invoice.getSubscription().getCustomer();

        Locale localeBrasil = Locale.of("pt", "BR");

        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(localeBrasil);

        emailService.sendEmail(
                customer.getEmail(),
                "Recibo da assinatura",
                "Olá, " + customer.getName() + ", segue o recibo de sua assinatura no valor de " + numberFormat.format(event.amount())
        );
    }
}
