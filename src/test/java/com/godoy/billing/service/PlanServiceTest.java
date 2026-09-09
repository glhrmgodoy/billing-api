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

import static org.assertj.core.api.Assertions.*;
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
        @DisplayName("deve mapear, salvar e retornar o plano criado")
        void shouldCreatePlan() {
            PlanRequest request = buildRequest();
            Plan planWithoutId = Plan.builder().name(request.name()).build();
            Plan savedPlan = buildPlan(UUID.randomUUID(), request.name(), true);
            PlanResponse response = buildResponse(savedPlan.getId(), request.name());

            when(planMapper.toEntity(request)).thenReturn(planWithoutId);
            when(planRepository.save(planWithoutId)).thenReturn(savedPlan);
            when(planMapper.toResponse(savedPlan)).thenReturn(response);

            PlanResponse result = planService.create(request);

            assertThat(result).isEqualTo(response);
            verify(planRepository).save(planWithoutId);
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("deve retornar todos os planos ativos mapeados")
        void shouldListActivePlans() {
            Plan plan1 = buildPlan(UUID.randomUUID(), "Basic", true);
            Plan plan2 = buildPlan(UUID.randomUUID(), "Pro", true);
            PlanResponse response1 = buildResponse(plan1.getId(), "Basic");
            PlanResponse response2 = buildResponse(plan2.getId(), "Pro");

            when(planRepository.findByActiveTrue()).thenReturn(List.of(plan1, plan2));
            when(planMapper.toResponse(plan1)).thenReturn(response1);
            when(planMapper.toResponse(plan2)).thenReturn(response2);

            List<PlanResponse> result = planService.findAllActive();

            assertThat(result).hasSize(2).containsExactlyInAnyOrder(response1, response2);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não houver planos ativos")
        void shouldReturnEmptyListWhenNoActivePlans() {
            when(planRepository.findByActiveTrue()).thenReturn(List.of());

            List<PlanResponse> result = planService.findAllActive();

            assertThat(result).isEmpty();
            verifyNoInteractions(planMapper);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar o plano quando o id existir")
        void shouldReturnPlanWhenIdExists() {
            UUID id = UUID.randomUUID();
            Plan plan = buildPlan(id, "Pro", true);
            PlanResponse response = buildResponse(id, "Pro");

            when(planRepository.findById(id)).thenReturn(Optional.of(plan));
            when(planMapper.toResponse(plan)).thenReturn(response);

            assertThat(planService.findById(id)).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o id não existir")
        void shouldThrowNotFoundExceptionWhenPlanDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(planRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.findById(id))
                    .isInstanceOf(NotFoundException.class);


            verifyNoInteractions(planMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve atualizar os campos do plano sem chamar save() explicitamente")
        void shouldUpdatePlanViaDirtyChecking() {
            UUID id = UUID.randomUUID();
            Plan existingPlan = buildPlan(id, "Old Name", true);
            PlanRequest request = new PlanRequest("New Name", BigDecimal.valueOf(49.90), BillingCycle.YEARLY);
            PlanResponse response = buildResponse(id, "New Name");

            when(planRepository.findById(id)).thenReturn(Optional.of(existingPlan));
            when(planMapper.toResponse(existingPlan)).thenReturn(response);

            PlanResponse result = planService.update(id, request);

            assertThat(result).isEqualTo(response);
            assertThat(existingPlan.getName()).isEqualTo("New Name");
            assertThat(existingPlan.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.90));
            assertThat(existingPlan.getBillingCycle()).isEqualTo(BillingCycle.YEARLY);

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve alterar o campo active através do update genérico")
        void shouldNotChangeActiveFieldOnUpdate() {
            UUID id = UUID.randomUUID();
            Plan existingPlan = buildPlan(id, "Name", true);
            PlanRequest request = buildRequest();

            when(planRepository.findById(id)).thenReturn(Optional.of(existingPlan));
            when(planMapper.toResponse(existingPlan)).thenReturn(buildResponse(id, "Name"));

            planService.update(id, request);

            assertThat(existingPlan.getActive()).isTrue();
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o plano não existir")
        void shouldThrowNotFoundExceptionWhenPlanDoesNotExistOnUpdate() {
            UUID id = UUID.randomUUID();
            PlanRequest request = buildRequest();

            when(planRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.update(id, request))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(planMapper);
        }
    }

    @Nested
    @DisplayName("inactivate")
    class Inactivate {

        @Test
        @DisplayName("deve marcar o plano como inativo sem chamar save() explicitamente")
        void shouldInactivatePlan() {
            UUID id = UUID.randomUUID();
            Plan plan = buildPlan(id, "Pro", true);

            when(planRepository.findById(id)).thenReturn(Optional.of(plan));

            planService.inactivate(id);

            assertThat(plan.getActive()).isFalse();
            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o plano não existir")
        void shouldThrowNotFoundExceptionWhenPlanDoesNotExistOnInactivate() {
            UUID id = UUID.randomUUID();
            when(planRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> planService.inactivate(id))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}