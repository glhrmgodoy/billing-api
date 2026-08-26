package com.godoy.billing.controller;

import com.godoy.billing.dto.request.SubscriptionRequest;
import com.godoy.billing.dto.response.SubscriptionResponse;
import com.godoy.billing.security.CustomerPrincipal;
import com.godoy.billing.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.subscribe(principal.getCustomer(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> findMe(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(subscriptionService.findMine(principal.getCustomer()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable UUID id
    ) {
        subscriptionService.cancel(principal.getCustomer(), id);
        return ResponseEntity.noContent().build();
    }
}
