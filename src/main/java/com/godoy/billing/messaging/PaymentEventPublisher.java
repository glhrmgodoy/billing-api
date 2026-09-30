package com.godoy.billing.messaging;

import com.godoy.billing.event.PaymentConfirmedEvent;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.exception.SdkException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final SnsTemplate snsTemplate;

    @Value("${aws.sns.payment-confirmed-topic-arn}")
    private String topicArn;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        try {
            snsTemplate.sendNotification(topicArn, event, "Pagamento confirmado");
        } catch (SdkException e) {
            log.error("Falha ao publicar evento de confirmação do pagamento. ID do pagamento: {}", event.paymentId(), e);
        }
    }
}
