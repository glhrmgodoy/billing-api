package com.godoy.billing.messaging;

import com.godoy.billing.event.PaymentConfirmedEvent;
import com.godoy.billing.service.PaymentReceiptService;
import io.awspring.cloud.sqs.annotation.SnsNotificationMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReceiptListener {

    private final PaymentReceiptService paymentReceiptService;

    @SqsListener(value = "${aws.sqs.payment-receipt-queue-name}")
    public void onPaymentConfirmed(@SnsNotificationMessage PaymentConfirmedEvent event) {
        paymentReceiptService.sendReceipt(event);
    }
}
