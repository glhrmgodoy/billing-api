package com.godoy.billing.controller;

import com.godoy.billing.dto.request.PaymentWebhookRequest;
import com.godoy.billing.dto.response.PaymentResponse;
import com.godoy.billing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<PaymentResponse> confirmPayment(@Valid @RequestBody PaymentWebhookRequest request) {
        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
