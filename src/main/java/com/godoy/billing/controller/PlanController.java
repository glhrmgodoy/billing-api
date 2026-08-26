package com.godoy.billing.controller;

import com.godoy.billing.dto.request.PlanRequest;
import com.godoy.billing.dto.response.PlanResponse;
import com.godoy.billing.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        PlanResponse response = planService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> findAllActive() {
        return ResponseEntity.ok(planService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PlanRequest request
    ) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @PatchMapping("/{id}/inactivate")
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        planService.inactivate(id);
        return ResponseEntity.noContent().build();
    }
}
