package com.godoy.billing.controller;

import com.godoy.billing.dto.response.CustomerResponse;
import com.godoy.billing.security.CustomerPrincipal;
import com.godoy.billing.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> findMe(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(customerService.findById(principal.getCustomer().getId()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> inactivateMe(@AuthenticationPrincipal CustomerPrincipal principal) {
        customerService.inactivate(principal.getCustomer().getId());
        return ResponseEntity.noContent().build();
    }
}
