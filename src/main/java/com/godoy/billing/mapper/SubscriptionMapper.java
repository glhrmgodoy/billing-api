package com.godoy.billing.mapper;

import com.godoy.billing.domain.entity.Subscription;
import com.godoy.billing.dto.response.SubscriptionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "Spring")
public interface SubscriptionMapper {

    @Mapping(target = "planName", source = "plan.name")
    SubscriptionResponse toResponse(Subscription subscription);
}
