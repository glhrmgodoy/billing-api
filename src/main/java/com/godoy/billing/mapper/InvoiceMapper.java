package com.godoy.billing.mapper;

import com.godoy.billing.domain.entity.Invoice;
import com.godoy.billing.dto.response.InvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "Spring")
public interface InvoiceMapper {

    @Mapping(target = "subscriptionId", source = "subscription.id")
    InvoiceResponse toResponse(Invoice invoice);
}
