package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.domain.entity.Plan;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import com.godoy.billing.dto.request.SubscriptionRequest;
import com.godoy.billing.dto.response.SubscriptionResponse;
import com.godoy.billing.event.SubscriptionCancelledEvent;
import com.godoy.billing.event.SubscriptionConfirmedEvent;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.SubscriptionMapper;
import com.godoy.billing.repository.PlanRepository;
import com.godoy.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public SubscriptionResponse subscribe(Customer customer, SubscriptionRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> NotFoundException.of("Plano", request.planId()));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new BusinessException("Esse plano não está mais disponível");
        }

        if (subscriptionRepository.existsByCustomerIdAndStatus(customer.getId(), SubscriptionStatus.ACTIVE)) {
            throw new BusinessException("Você já possui uma assinatura ativa");
        }

        LocalDate cycleStart = LocalDate.now();
        LocalDate cycleEnd = plan.getBillingCycle().advance(cycleStart);

        Subscription subscription = Subscription.builder()
                .customer(customer)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .currentCycleStart(cycleStart)
                .currentCycleEnd(cycleEnd)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);

        applicationEventPublisher.publishEvent(
                new SubscriptionConfirmedEvent(
                        saved.getId(),
                        customer.getEmail(),
                        customer.getName(),
                        plan.getName()
                )
        );

        return subscriptionMapper.toResponse(saved);
    }

    public SubscriptionResponse findMine(Customer customer) {
        Subscription subscription = subscriptionRepository
                .findByCustomerIdAndStatusNot(customer.getId(), SubscriptionStatus.CANCELLED)
                .orElseThrow(() -> new NotFoundException("Você não possui nenhuma assinatura"));

        return subscriptionMapper.toResponse(subscription);
    }

    @Transactional
    public void cancel(Customer customer, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> NotFoundException.of("Assinatura", subscriptionId));

        if (!subscription.getCustomer().getId().equals(customer.getId())) {
            throw NotFoundException.of("Assinatura", subscriptionId);
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BusinessException("Esta assinatura já está cancelada");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);

        applicationEventPublisher.publishEvent(
                new SubscriptionCancelledEvent(
                        subscription.getId(),
                        customer.getEmail(), customer.getName(),
                        subscription.getPlan().getName(),
                        subscription.getCurrentCycleEnd()
                )
        );
    }
}
