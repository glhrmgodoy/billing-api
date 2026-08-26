package com.godoy.billing.mapper;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.dto.request.CustomerRequest;
import com.godoy.billing.dto.response.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "Spring")
public interface CustomerMapper {

    @Mapping(target = "password", ignore = true)
    Customer toEntity(CustomerRequest request);

    CustomerResponse toResponse(Customer customer);
}
