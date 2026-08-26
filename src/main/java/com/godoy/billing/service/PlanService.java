package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Plan;
import com.godoy.billing.dto.request.PlanRequest;
import com.godoy.billing.dto.response.PlanResponse;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.PlanMapper;
import com.godoy.billing.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;

    @Transactional
    public PlanResponse create(PlanRequest request) {
        Plan plan = planMapper.toEntity(request);
        Plan saved = planRepository.save(plan);
        return planMapper.toResponse(saved);
    }

    public List<PlanResponse> findAllActive() {
        return planRepository.findByActiveTrue()
                .stream()
                .map(planMapper::toResponse)
                .toList();
    }

    public PlanResponse findById(UUID id) {
        return planMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        Plan plan = findEntityById(id);

        plan.setName(request.name());
        plan.setPrice(request.price());
        plan.setBillingCycle(request.billingCycle());

        return planMapper.toResponse(plan);
    }

    @Transactional
    public void inactivate(UUID id) {
        Plan plan = findEntityById(id);

        plan.setActive(false);
    }

    private Plan findEntityById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Plano", id));
    }
}
