package com.godoy.billing.controller;

import com.godoy.billing.dto.response.InvoiceResponse;
import com.godoy.billing.security.CustomerPrincipal;
import com.godoy.billing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/me")
    public ResponseEntity<List<InvoiceResponse>> findMe(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(invoiceService.findMine(principal.getCustomer()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(invoiceService.findById(principal.getCustomer(), id));
    }
}
