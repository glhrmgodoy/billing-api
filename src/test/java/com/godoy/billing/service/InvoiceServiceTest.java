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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        @DisplayName("should return the customer's invoices mapped")
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

            assertEquals(1, result.size());
            assertEquals(response, result.get(0));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return the invoice when it belongs to the customer")
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

            assertEquals(response, invoiceService.findById(customer, invoice.getId()));
        }

        @Test
        @DisplayName("should throw NotFoundException when the invoice does not exist")
        void shouldThrowExceptionWhenInvoiceDoesNotExist() {
            Customer customer = buildCustomer(UUID.randomUUID());
            UUID invoiceId = UUID.randomUUID();
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> invoiceService.findById(customer, invoiceId));
        }

        @Test
        @DisplayName("should throw NotFoundException when the invoice belongs to another customer")
        void shouldThrowExceptionWhenInvoiceBelongsToAnotherCustomer() {
            Customer owner = buildCustomer(UUID.randomUUID());
            Customer otherCustomer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            Subscription subscription = buildSubscription(owner, plan, LocalDate.now());
            Invoice invoice = buildInvoice(subscription, InvoiceStatus.PENDING, LocalDate.now().plusDays(5));

            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

            assertThrows(NotFoundException.class, () -> invoiceService.findById(otherCustomer, invoice.getId()));
        }
    }

    @Nested
    @DisplayName("generateInvoicesForDueSubscriptions")
    class GenerateInvoices {

        @Test
        @DisplayName("should generate an invoice and advance the cycle for monthly subscriptions due today")
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

            assertEquals(subscription, generated.getSubscription());
            assertEquals(plan.getPrice(), generated.getAmount());
            assertEquals(InvoiceStatus.PENDING, generated.getStatus());
            assertEquals(currentCycleEnd.plusDays(5), generated.getDueDate());

            assertEquals(currentCycleEnd, subscription.getCurrentCycleStart());
            assertEquals(currentCycleEnd.plusMonths(1), subscription.getCurrentCycleEnd());
        }

        @Test
        @DisplayName("should not generate any invoice when no subscriptions are due")
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
        @DisplayName("should mark the invoice as OVERDUE and suspend the subscription")
        void shouldMarkInvoiceOverdueAndSuspendSubscription() {
            Customer customer = buildCustomer(UUID.randomUUID());
            Plan plan = buildPlan(BillingCycle.MONTHLY);
            Subscription subscription = buildSubscription(customer, plan, LocalDate.now());
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            Invoice invoice = buildInvoice(subscription, InvoiceStatus.PENDING, LocalDate.now().minusDays(1));

            when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, LocalDate.now()))
                    .thenReturn(List.of(invoice));

            invoiceService.markOverdueInvoices();

            assertEquals(InvoiceStatus.PENDING, invoice.getStatus());
            assertEquals(SubscriptionStatus.SUSPENDED, subscription.getStatus());
        }

        @Test
        @DisplayName("should not change anything when there are no overdue invoices")
        void shouldNotChangeAnythingWhenNoOverdueInvoices() {
            when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, LocalDate.now()))
                    .thenReturn(List.of());

            invoiceService.markOverdueInvoices();

            verifyNoMoreInteractions(invoiceRepository);
        }
    }
}