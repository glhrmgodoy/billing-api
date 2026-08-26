package com.godoy.billing.repository;

import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.domain.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findBySubscription_Customer_Id(UUID customerId);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);
}
