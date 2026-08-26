package com.godoy.billing.mapper;

import com.godoy.billing.domain.entity.Payment;
import com.godoy.billing.dto.response.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "Spring")
public interface PaymentMapper {

    @Mapping(target = "invoiceId", source = "invoice.id")
    PaymentResponse toResponse(Payment payment);
}
