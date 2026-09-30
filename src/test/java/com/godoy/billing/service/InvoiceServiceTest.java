package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.entity.Plan;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.BillingCycle;
import com.godoy.billing.domain.enums.InvoiceStatus;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import com.godoy.billing.dto.response.InvoiceResponse;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.InvoiceMapper;
import com.godoy.billing.repository.InvoiceRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private InvoiceService invoiceService;

    private Customer buildCustomer(UUID id) {
        return Customer.builder().id(id).name("Guilherme").email("g@email.com").active(true).build();
    }

    private Plan buildPlan(BillingCycle cycle) {
        return Plan.builder().id(UUID.randomUUID()).name("Pro").price(BigDecimal.valueOf(29.90))
                .billingCycle(cycle).active(true).build();
    }

    private Subscription buildSubscription(Customer customer, Plan plan, LocalDate cycleEnd) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .currentCycleStart(cycleEnd.minusMonths(1))
                .currentCycleEnd(cycleEnd)
                .build();
    }

    private Invoice buildInvoice(Subscription subscription, InvoiceStatus status, LocalDate dueDate) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .subscription(subscription)
                .amount(subscription.getPlan().getPrice())
                .dueDate(dueDate)
                .status(status)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findMine")
    class FindMine {

        @Test
        @DisplayName("deve retornar as faturas do cliente mapeadas")
        void shouldReturnCustomerInvoices() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            Subscription subscription = buildSubscription(customer, plan, LocalDate.now());
            Invoice invoice = buildInvoice(subscription, InvoiceStatus.PENDING, LocalDate.now().plusDays(5));
            InvoiceResponse response = new InvoiceResponse(
                    invoice.getId(), subscription.getId(), invoice.getAmount(), invoice.getDueDate(),
                    InvoiceStatus.PENDING, LocalDateTime.now());

            when(invoiceRepository.findBySubscription_Customer_Id(customer.getId())).thenReturn(List.of(invoice));
            when(invoiceMapper.toResponse(invoice)).thenReturn(response);

            List<InvoiceResponse> result = invoiceService.findMine(customer);

            assertThat(result).hasSize(1).containsExactly(response);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar a fatura quando pertencer ao cliente")
        void shouldReturnInvoiceOwnedByCustomer() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            Subscription subscription = buildSubscription(customer, plan, LocalDate.now());
            Invoice invoice = buildInvoice(subscription, InvoiceStatus.PENDING, LocalDate.now().plusDays(5));
            InvoiceResponse response = new InvoiceResponse(
                    invoice.getId(), subscription.getId(), invoice.getAmount(), invoice.getDueDate(),
                    InvoiceStatus.PENDING, LocalDateTime.now());

            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
            when(invoiceMapper.toResponse(invoice)).thenReturn(response);

            assertThat(invoiceService.findById(customer, invoice.getId())).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando a fatura não existir")
        void shouldThrowExceptionWhenInvoiceDoesNotExist() {
            Customer customer = buildCustomer(UUID.randomUUID());
            UUID invoiceId = UUID.randomUUID();
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.findById(customer, invoiceId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando a fatura pertencer a outro cliente")
        void shouldThrowExceptionWhenInvoiceBelongsToAnotherCustomer() {
            Customer owner = buildCustomer(UUID.randomUUID());
            Customer otherCustomer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            Subscription subscription = buildSubscription(owner, plan, LocalDate.now());
            Invoice invoice = buildInvoice(subscription, InvoiceStatus.PENDING, LocalDate.now().plusDays(5));

            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> invoiceService.findById(otherCustomer, invoice.getId()))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("generateInvoicesForDueSubscriptions")
    class GenerateInvoices {

        @Test
        @DisplayName("deve gerar fatura e avançar o ciclo para assinaturas mensais vencendo hoje")
        void shouldGenerateInvoiceAndAdvanceMonthlyCycle() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            LocalDate currentCycleEnd = LocalDate.now();
            Subscription subscription = buildSubscription(customer, plan, currentCycleEnd);

            when(subscriptionRepository.findByStatusAndCurrentCycleEndLessThanEqual(
                    SubscriptionStatus.ACTIVE, LocalDate.now()))
                    .thenReturn(List.of(subscription));

            ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);

            invoiceService.generateInvoicesForDueSubscriptions();

            verify(invoiceRepository).save(captor.capture());
            Invoice generated = captor.getValue();

            assertThat(generated.getSubscription()).isEqualTo(subscription);
            assertThat(generated.getAmount()).isEqualByComparingTo(plan.getPrice());
            assertThat(generated.getStatus()).isEqualTo(InvoiceStatus.PENDING);
            assertThat(generated.getDueDate()).isEqualTo(currentCycleEnd.plusDays(5));

            assertThat(subscription.getCurrentCycleStart()).isEqualTo(currentCycleEnd);
            assertThat(subscription.getCurrentCycleEnd()).isEqualTo(currentCycleEnd.plusMonths(1));
        }

        @Test
        @DisplayName("não deve gerar nenhuma fatura quando não houver assinaturas vencendo")
        void shouldNotGenerateInvoiceWhenNoSubscriptionsAreDue() {
            when(subscriptionRepository.findByStatusAndCurrentCycleEndLessThanEqual(
                    SubscriptionStatus.ACTIVE, LocalDate.now()))
                    .thenReturn(List.of());

            invoiceService.generateInvoicesForDueSubscriptions();

            verify(invoiceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markOverdueInvoices")
    class MarkOverdue {

        @Test
        @DisplayName("deve marcar fatura como OVERDUE e suspender a assinatura")
        void shouldMarkInvoiceOverdueAndSuspendSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            Subscription subscription = buildSubscription(customer, plan, LocalDate.now());
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            Invoice invoice = buildInvoice(subscription, InvoiceStatus.PENDING, LocalDate.now().minusDays(1));

            when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, LocalDate.now()))
                    .thenReturn(List.of(invoice));

            invoiceService.markOverdueInvoices();

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        @DisplayName("não deve alterar nada quando não houver faturas vencidas")
        void shouldNotChangeAnythingWhenNoOverdueInvoices() {
            when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, LocalDate.now()))
                    .thenReturn(List.of());

            invoiceService.markOverdueInvoices();

            verifyNoMoreInteractions(invoiceRepository);
        }
    }
}