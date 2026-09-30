package com.godoy.billing.messaging;

import com.godoy.billing.domain.enums.PaymentMessageConsumer;
import com.godoy.billing.event.PaymentConfirmedEvent;
import com.godoy.billing.service.PaymentMessageDeduplicationService;
import io.awspring.cloud.sqs.annotation.SnsNotificationMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingListener {

    private final PaymentMessageDeduplicationService paymentMessageDeduplicationService;

    @SqsListener(value = "${aws.sqs.payment-processing-queue-name}")
    public void onProcessPayment(@SnsNotificationMessage PaymentConfirmedEvent event) {
        if (paymentMessageDeduplicationService.tryMarkAsProcessed(event.paymentId(), PaymentMessageConsumer.PAYMENT_PROCESSING)) {
            log.info("Processando pagamento {} (fatura {}, valor {})", event.paymentId(), event.invoiceId(), event.amount());
        }
    }
}
