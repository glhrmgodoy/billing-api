package com.godoy.billing.mapper;

import com.godoy.billing.domain.entity.Plan;
import com.godoy.billing.dto.request.PlanRequest;
import com.godoy.billing.dto.response.PlanResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface PlanMapper {

    Plan toEntity(PlanRequest request);

    PlanResponse toResponse(Plan plan);
}
