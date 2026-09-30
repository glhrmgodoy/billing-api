package com.godoy.billing.event;

import com.godoy.billing.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceOverdue(InvoiceOverdueEvent event) {
        try {
            emailService.sendEmail(
                    event.customerEmail(),
                    "Sua fatura venceu",
                    "Olá " + event.customerName() + ", Sua fatura, no valor de " + event.amount() + ", referente ao plano " + event.planName() + ", está em atraso desde " + event.dueDate() + "."
            );
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de fatura vencida {}", event.invoiceId(), e);
        }
    }
}
