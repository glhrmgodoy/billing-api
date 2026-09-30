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
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
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

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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
        @DisplayName("deve criar assinatura com ciclo mensal calculado corretamente")
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

            assertThat(result).isEqualTo(response);
            assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(saved.getCurrentCycleStart()).isEqualTo(LocalDate.now());
            assertThat(saved.getCurrentCycleEnd()).isEqualTo(LocalDate.now().plusMonths(1));
        }

        @Test
        @DisplayName("deve calcular o ciclo anual corretamente para plano YEARLY")
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
            assertThat(captor.getValue().getCurrentCycleEnd()).isEqualTo(LocalDate.now().plusYears(1));
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o plano não existir")
        void shouldThrowExceptionWhenPlanDoesNotExist() {
            UUID planId = UUID.randomUUID();
            Customer customer = buildCustomer(UUID.randomUUID());
            SubscriptionRequest request = new SubscriptionRequest(planId);

            when(planRepository.findById(planId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.subscribe(customer, request))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(subscriptionRepository, subscriptionMapper);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando o plano estiver inativo")
        void shouldThrowExceptionWhenPlanIsInactive() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, false);
            SubscriptionRequest request = new SubscriptionRequest(plan.getId());

            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

            assertThatThrownBy(() -> subscriptionService.subscribe(customer, request))
                    .isInstanceOf(BusinessException.class);
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar BusinessException quando o cliente já tiver assinatura ativa")
        void shouldThrowExceptionWhenCustomerAlreadyHasActiveSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            SubscriptionRequest request = new SubscriptionRequest(plan.getId());

            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndStatus(customer.getId(), SubscriptionStatus.ACTIVE))
                    .thenReturn(true);

            assertThatThrownBy(() -> subscriptionService.subscribe(customer, request))
                    .isInstanceOf(BusinessException.class);
            verify(subscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findMine")
    class FindMine {

        @Test
        @DisplayName("deve retornar a assinatura vigente do cliente")
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

            assertThat(subscriptionService.findMine(customer)).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o cliente não tiver assinatura")
        void shouldThrowExceptionWhenNoSubscriptionExists() {
            Customer customer = buildCustomer(UUID.randomUUID());
            when(subscriptionRepository.findByCustomerIdAndStatusNot(customer.getId(), SubscriptionStatus.CANCELLED))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.findMine(customer))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("deve cancelar a assinatura sem alterar currentCycleEnd")
        void shouldCancelSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), customer, plan, SubscriptionStatus.ACTIVE);
            LocalDate originalCycleEnd = subscription.getCurrentCycleEnd();

            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

            subscriptionService.cancel(customer, subscription.getId());

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(subscription.getCurrentCycleEnd()).isEqualTo(originalCycleEnd);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando a assinatura não existir")
        void shouldThrowExceptionWhenSubscriptionDoesNotExist() {
            Customer customer = buildCustomer(UUID.randomUUID());
            UUID subscriptionId = UUID.randomUUID();
            when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.cancel(customer, subscriptionId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando a assinatura pertencer a outro cliente")
        void shouldThrowExceptionWhenSubscriptionBelongsToAnotherCustomer() {
            Customer owner = buildCustomer(UUID.randomUUID());
            Customer otherCustomer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), owner, plan, SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.cancel(otherCustomer, subscription.getId()))
                    .isInstanceOf(NotFoundException.class);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando a assinatura já estiver cancelada")
        void shouldThrowExceptionWhenAlreadyCanceled() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(UUID.randomUUID(), BillingCycle.MONTHLY, true);
            Subscription subscription = buildSubscription(UUID.randomUUID(), customer, plan, SubscriptionStatus.CANCELLED);

            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.cancel(customer, subscription.getId()))
                    .isInstanceOf(BusinessException.class);
        }
    }
}