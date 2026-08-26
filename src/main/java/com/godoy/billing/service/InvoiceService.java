package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.InvoiceStatus;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import com.godoy.billing.dto.response.InvoiceResponse;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.InvoiceMapper;
import com.godoy.billing.repository.InvoiceRepository;
import com.godoy.billing.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private static final int DUE_DATE_GRACE_DAYS = 5;

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceMapper invoiceMapper;

    public List<InvoiceResponse> findMine(Customer customer) {
        return invoiceRepository.findBySubscription_Customer_Id(customer.getId())
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    public InvoiceResponse findById(Customer customer, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> NotFoundException.of("Fatura", invoiceId));

        if (!invoice.getSubscription().getCustomer().getId().equals(customer.getId())) {
            throw NotFoundException.of("Fatura", invoiceId);
        }

        return invoiceMapper.toResponse(invoice);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void generateInvoiceForDueSubscription() {
        LocalDate today = LocalDate.now();

        List<Subscription> dueSubscriptions = subscriptionRepository
                .findByStatusAndCurrentCycleEndLessThanEqual(SubscriptionStatus.ACTIVE, today);

        log.info("Job de geração de fatura: {} assinatura(s) vencendo hoje", dueSubscriptions.size());

        for (Subscription subscription : dueSubscriptions) {
            Invoice invoice = Invoice.builder()
                    .subscription(subscription)
                    .amount(subscription.getPlan().getPrice())
                    .dueDate(subscription.getCurrentCycleEnd().plusDays(DUE_DATE_GRACE_DAYS))
                    .status(InvoiceStatus.PENDING)
                    .build();
            invoiceRepository.save(invoice);

        LocalDate newCycleStart = subscription.getCurrentCycleEnd();
        subscription.setCurrentCycleStart(newCycleStart);
        subscription.setCurrentCycleEnd(subscription.getPlan().getBillingCycle().advance(newCycleStart));
        }
    }

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();

        List<Invoice> overdueInvoices = invoiceRepository
                .findByStatusAndDueDateBefore(InvoiceStatus.PENDING, today);

        log.info("Job de inadimplência: {} fatura(s) vencida(s) sem pagamento", overdueInvoices.size());

        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus.PENDING);

            Subscription subscription = invoice.getSubscription();
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
        }
    }
}
