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
public class SubscriptionEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionConfirmed(SubscriptionConfirmedEvent event) {
        try {
            emailService.sendEmail(
                    event.customerEmail(),
                    "Sua assinatura " + event.planName() + " está confirmada",
                    "Obrigado por iniciar o seu plano " + event.planName() + ", " + event.customerName() + "."
            );
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de confirmação para assinatura {}", event.subscriptionId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionCancelled(SubscriptionCancelledEvent event) {
        try {
            emailService.sendEmail(
                    event.customerEmail(),
                    "Sua assinatura " + event.planName() + " foi cancelada",
                    "Sua assinatura " + event.planName() + " está cancelada, e você perderá os benefícios em " + event.cycleEnd() + "."
            );
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de cancelamento da assinatura {}", event.subscriptionId(), e);
        }
    }
}
