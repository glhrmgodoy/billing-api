package com.godoy.billing.repository;

import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByCustomerIdAndStatusNot(UUID customerId, SubscriptionStatus status);

    boolean existsByCustomerIdAndStatus(UUID customerId, SubscriptionStatus status);

    List<Subscription> findByStatusAndCurrentCycleEndLessThanEqual(SubscriptionStatus status, LocalDate date);
}
