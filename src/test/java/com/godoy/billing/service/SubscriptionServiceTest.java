package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.domain.entity.Plan;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.BillingCycle;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import com.godoy.billing.dto.request.SubscriptionRequest;
import com.godoy.billing.dto.response.SubscriptionResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.SubscriptionMapper;
import com.godoy.billing.repository.PlanRepository;
import com.godoy.billing.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Customer buildCustomer(UUID id) {
        return Customer.builder().id(id).name("Guilherme").email("g@email.com").active(true).build();
    }

    private Plan buildPlan(UUID id, BillingCycle cycle, boolean active) {
        return Plan.builder()
                .id(id)
                .name("Pro")
                .price(BigDecimal.valueOf(29.90))
                .billingCycle(cycle)
                .active(active)
                .build();
    }

    private Subscription buildSubscription(UUID id, Customer customer, Plan plan, SubscriptionStatus status) {
        return Subscription.builder()
                .id(id)
                .customer(customer)
                .plan(plan)
                .status(status)
                .currentCycleStart(LocalDate.now())
                .currentCycleEnd(LocalDate.now().plusMonths(1))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("subscribe")
    class Subscribe {

        @Test
        @DisplayName("should create a subscription with a correctly calculated monthly cycle")
        void shouldCreateSubscriptionWithMonthlyCycle() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            SubscriptionRequest request = new SubscriptionRequest(plan.getId());
            SubscriptionResponse response = new SubscriptionResponse(
                    UUID.randomUUID(), plan.getName(), SubscriptionStatus.ACTIVE,
                    LocalDate.now(), LocalDate.now().plusMonths(1), LocalDateTime.now());

            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndStatus(customer.getId(), SubscriptionStatus.ACTIVE))
                    .thenReturn(false);
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(response);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);

            SubscriptionResponse result = subscriptionService.subscribe(customer, request);

            verify(subscriptionRepository).save(captor.capture());
            Subscription saved = captor.getValue();

            assertEquals(response, result);
            assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());
            assertEquals(LocalDate.now(), saved.getCurrentCycleStart());
            assertEquals(LocalDate.now().plusMonths(1), saved.getCurrentCycleEnd());
        }

        @Test
        @DisplayName("should correctly calculate the yearly cycle for a YEARLY plan")
        void shouldCreateSubscriptionWithYearlyCycle() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.YEARLY, true);
            SubscriptionRequest request = new SubscriptionRequest(plan.getId());

            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndStatus(customer.getId(), SubscriptionStatus.ACTIVE))
                    .thenReturn(false);
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(subscriptionMapper.toResponse(any(Subscription.class))).thenReturn(
                    new SubscriptionResponse(UUID.randomUUID(), plan.getName(), SubscriptionStatus.ACTIVE,
                            LocalDate.now(), LocalDate.now().plusYears(1), LocalDateTime.now()));

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);

            subscriptionService.subscribe(customer, request);

            verify(subscriptionRepository).save(captor.capture());
            assertEquals(LocalDate.now().plusYears(1), captor.getValue().getCurrentCycleEnd());
        }

        @Test
        @DisplayName("should throw NotFoundException when the plan does not exist")
        void shouldThrowExceptionWhenPlanDoesNotExist() {
            UUID planId = UUID.randomUUID();
            Customer customer = buildCustomer(UUID.randomUUID());
            SubscriptionRequest request = new SubscriptionRequest(planId);

            when(planRepository.findById(planId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> subscriptionService.subscribe(customer, request));
            verifyNoInteractions(subscriptionRepository, subscriptionMapper);
        }

        @Test
        @DisplayName("should throw BusinessException when the plan is inactive")
        void shouldThrowExceptionWhenPlanIsInactive() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, false);
            SubscriptionRequest request = new SubscriptionRequest(plan.getId());

            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

            assertThrows(BusinessException.class, () -> subscriptionService.subscribe(customer, request));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when the customer already has an active subscription")
        void shouldThrowExceptionWhenCustomerAlreadyHasActiveSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            SubscriptionRequest request = new SubscriptionRequest(plan.getId());

            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndStatus(customer.getId(), SubscriptionStatus.ACTIVE))
                    .thenReturn(true);

            assertThrows(BusinessException.class, () -> subscriptionService.subscribe(customer, request));
            verify(subscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findMine")
    class FindMine {

        @Test
        @DisplayName("should return the customer's current subscription")
        void shouldReturnCurrentSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), customer, plan, SubscriptionStatus.ACTIVE);
            SubscriptionResponse response = new SubscriptionResponse(
                    subscription.getId(), plan.getName(), SubscriptionStatus.ACTIVE,
                    subscription.getCurrentCycleStart(), subscription.getCurrentCycleEnd(), LocalDateTime.now());

            when(subscriptionRepository.findByCustomerIdAndStatusNot(customer.getId(), SubscriptionStatus.CANCELLED))
                    .thenReturn(Optional.of(subscription));
            when(subscriptionMapper.toResponse(subscription)).thenReturn(response);

            assertEquals(response, subscriptionService.findMine(customer));
        }

        @Test
        @DisplayName("should throw NotFoundException when the customer has no subscription")
        void shouldThrowExceptionWhenNoSubscriptionExists() {
            Customer customer = buildCustomer(UUID.randomUUID());
            when(subscriptionRepository.findByCustomerIdAndStatusNot(customer.getId(), SubscriptionStatus.CANCELLED))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> subscriptionService.findMine(customer));
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("should cancel the subscription without changing currentCycleEnd")
        void shouldCancelSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), customer, plan, SubscriptionStatus.ACTIVE);
            LocalDate originalCycleEnd = subscription.getCurrentCycleEnd();

            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

            subscriptionService.cancel(customer, subscription.getId());

            assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
            assertEquals(originalCycleEnd, subscription.getCurrentCycleEnd());
        }

        @Test
        @DisplayName("should throw NotFoundException when the subscription does not exist")
        void shouldThrowExceptionWhenSubscriptionDoesNotExist() {
            Customer customer = buildCustomer(UUID.randomUUID());
            UUID subscriptionId = UUID.randomUUID();
            when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> subscriptionService.cancel(customer, subscriptionId));
        }

        @Test
        @DisplayName("should throw NotFoundException when the subscription belongs to another customer")
        void shouldThrowExceptionWhenSubscriptionBelongsToAnotherCustomer() {
            Customer owner = buildCustomer(UUID.randomUUID());
            Customer otherCustomer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), owner, plan, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

            assertThrows(NotFoundException.class,
                    () -> subscriptionService.cancel(otherCustomer, subscription.getId()));
            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        }

        @Test
        @DisplayName("should throw BusinessException when the subscription is already canceled")
        void shouldThrowExceptionWhenAlreadyCanceled() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), customer, plan, SubscriptionStatus.CANCELLED);

            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

            assertThrows(BusinessException.class, () -> subscriptionService.cancel(customer, subscription.getId()));
        }
    }
}