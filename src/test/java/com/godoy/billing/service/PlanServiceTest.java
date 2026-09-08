package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Plan;
import com.godoy.billing.domain.enums.BillingCycle;
import com.godoy.billing.dto.request.PlanRequest;
import com.godoy.billing.dto.response.PlanResponse;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.PlanMapper;
import com.godoy.billing.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

    @InjectMocks
    private PlanService planService;

    private Plan buildPlan(UUID id, String name, boolean active) {
        return Plan.builder()
                .id(id)
                .name(name)
                .price(BigDecimal.valueOf(29.90))
                .billingCycle(BillingCycle.MONTHLY)
                .active(active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PlanRequest buildRequest() {
        return new PlanRequest("Pro Plan", BigDecimal.valueOf(29.90), BillingCycle.MONTHLY);
    }

    private PlanResponse buildResponse(UUID id, String name) {
        return new PlanResponse(id, name, BigDecimal.valueOf(29.90), BillingCycle.MONTHLY, true, LocalDateTime.now());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should map, save and return the created plan")
        void shouldCreatePlan() {
            PlanRequest request = buildRequest();
            Plan planWithoutId = Plan.builder().name(request.name()).build();
            Plan savedPlan = buildPlan(UUID.randomUUID(), request.name(), true);
            PlanResponse response = buildResponse(savedPlan.getId(), request.name());

            when(planMapper.toEntity(request)).thenReturn(planWithoutId);
            when(planRepository.save(planWithoutId)).thenReturn(savedPlan);
            when(planMapper.toResponse(savedPlan)).thenReturn(response);

            PlanResponse result = planService.create(request);

            assertEquals(response, result);
            verify(planRepository).save(planWithoutId);
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("should return all active plans mapped")
        void shouldListActivePlans() {
            Plan plan1 = buildPlan(UUID.randomUUID(), "Basic", true);
            Plan plan2 = buildPlan(UUID.randomUUID(), "Pro", true);
            PlanResponse response1 = buildResponse(plan1.getId(), "Basic");
            PlanResponse response2 = buildResponse(plan2.getId(), "Pro");

            when(planRepository.findByActiveTrue()).thenReturn(List.of(plan1, plan2));
            when(planMapper.toResponse(plan1)).thenReturn(response1);
            when(planMapper.toResponse(plan2)).thenReturn(response2);

            List<PlanResponse> result = planService.findAllActive();

            assertEquals(2, result.size());
            assertTrue(result.containsAll(List.of(response1, response2)));
        }

        @Test
        @DisplayName("should return an empty list when there are no active plans")
        void shouldReturnEmptyListWhenNoActivePlans() {
            when(planRepository.findByActiveTrue()).thenReturn(List.of());

            List<PlanResponse> result = planService.findAllActive();

            assertTrue(result.isEmpty());
            verifyNoInteractions(planMapper);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return the plan when the id exists")
        void shouldReturnPlanWhenIdExists() {
            UUID id = UUID.randomUUID();
            Plan plan = buildPlan(id, "Pro", true);
            PlanResponse response = buildResponse(id, "Pro");

            when(planRepository.findById(id)).thenReturn(Optional.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(response);

            PlanResponse result = planService.findById(id);

            assertEquals(response, result);
        }

        @Test
        @DisplayName("should throw NotFoundException when the id does not exist")
        void shouldThrowNotFoundExceptionWhenPlanDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(planRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> planService.findById(id));

            verifyNoInteractions(planMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update the plan fields without calling save() explicitly")
        void shouldUpdatePlanViaDirtyChecking() {
            UUID id = UUID.randomUUID();
            Plan existingPlan = buildPlan(id, "Old Name", true);
            PlanRequest request = new PlanRequest("New Name", BigDecimal.valueOf(49.90), BillingCycle.YEARLY);
            PlanResponse response = buildResponse(id, "New Name");

            when(planRepository.findById(id)).thenReturn(Optional.of(existingPlan));
            when(planMapper.toResponse(existingPlan)).thenReturn(response);

            PlanResponse result = planService.update(id, request);

            assertEquals(response, result);
            assertEquals("New Name", existingPlan.getName());
            assertEquals(BigDecimal.valueOf(49.90), existingPlan.getPrice());
            assertEquals(BillingCycle.YEARLY, existingPlan.getBillingCycle());

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not change the active field through the generic update")
        void shouldNotChangeActiveFieldOnUpdate() {
            UUID id = UUID.randomUUID();
            Plan existingPlan = buildPlan(id, "Name", true);
            PlanRequest request = buildRequest();

            when(planRepository.findById(id)).thenReturn(Optional.of(existingPlan));
            when(planMapper.toResponse(existingPlan)).thenReturn(buildResponse(id, "Name"));

            planService.update(id, request);

            assertTrue(existingPlan.getActive());
        }

        @Test
        @DisplayName("should throw NotFoundException when the plan does not exist")
        void shouldThrowNotFoundExceptionWhenPlanDoesNotExistOnUpdate() {
            UUID id = UUID.randomUUID();
            PlanRequest request = buildRequest();

            when(planRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> planService.update(id, request));
            verifyNoInteractions(planMapper);
        }
    }

    @Nested
    @DisplayName("inactivate")
    class Inactivate {

        @Test
        @DisplayName("should mark the plan as inactive without calling save() explicitly")
        void shouldInactivatePlan() {
            UUID id = UUID.randomUUID();
            Plan plan = buildPlan(id, "Pro", true);

            when(planRepository.findById(id)).thenReturn(Optional.of(plan));

            planService.inactivate(id);

            assertFalse(plan.getActive());
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when the plan does not exist")
        void shouldThrowNotFoundExceptionWhenPlanDoesNotExistOnInactivate() {
            UUID id = UUID.randomUUID();
            when(planRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> planService.inactivate(id));
        }
    }
}