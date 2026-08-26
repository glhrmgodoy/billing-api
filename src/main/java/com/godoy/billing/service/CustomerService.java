package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.dto.request.CustomerRequest;
import com.godoy.billing.dto.response.CustomerResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.CustomerMapper;
import com.godoy.billing.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CustomerResponse register(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessException("Já existe uma conta com este email");
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setPassword(passwordEncoder.encode(request.password()));

        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    public CustomerResponse findById(UUID id) {
        return customerMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public void inactivate(UUID id) {
        Customer customer = findEntityById(id);
        customer.setActive(false);
    }

    private Customer findEntityById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Cliente", id));
    }
}
