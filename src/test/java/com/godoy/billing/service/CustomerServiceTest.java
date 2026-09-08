package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.dto.request.CustomerRequest;
import com.godoy.billing.dto.response.CustomerResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.exception.NotFoundException;
import com.godoy.billing.mapper.CustomerMapper;
import com.godoy.billing.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    private Customer buildCustomer(UUID id, String email, boolean active) {
        return Customer.builder()
                .id(id)
                .name("Guilherme")
                .email(email)
                .password("some-hash")
                .active(active)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CustomerRequest buildRequest(String email) {
        return new CustomerRequest("Guilherme", email, "password12345");
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should hash the password before saving")
        void shouldHashPasswordBeforeSaving() {
            CustomerRequest request = buildRequest("guilherme@email.com");
            Customer customerWithoutPassword = Customer.builder().name(request.name()).email(request.email()).build();
            Customer savedCustomer = buildCustomer(UUID.randomUUID(), request.email(), true);
            CustomerResponse response = new CustomerResponse(
                    savedCustomer.getId(), request.name(), request.email(), true, LocalDateTime.now());

            when(customerRepository.existsByEmail(request.email())).thenReturn(false);
            when(customerMapper.toEntity(request)).thenReturn(customerWithoutPassword);
            when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
            when(customerRepository.save(customerWithoutPassword)).thenReturn(savedCustomer);
            when(customerMapper.toResponse(savedCustomer)).thenReturn(response);

            CustomerResponse result = customerService.register(request);

            assertEquals(response, result);
            assertEquals("encoded-password", customerWithoutPassword.getPassword());
            verify(passwordEncoder).encode("password12345");
        }

        @Test
        @DisplayName("should throw BusinessException when the email is already registered")
        void shouldThrowExceptionForDuplicateEmail() {
            CustomerRequest request = buildRequest("guilherme@email.com");
            when(customerRepository.existsByEmail(request.email())).thenReturn(true);

            assertThrows(BusinessException.class, () -> customerService.register(request));

            verifyNoInteractions(passwordEncoder, customerMapper);
            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return the customer when the id exists")
        void shouldReturnExistingCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id, "guilherme@email.com", true);
            CustomerResponse response = new CustomerResponse(id, "Guilherme", "guilherme@email.com", true, LocalDateTime.now());

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerMapper.toResponse(customer)).thenReturn(response);

            assertEquals(response, customerService.findById(id));
        }

        @Test
        @DisplayName("should throw NotFoundException when the id does not exist")
        void shouldThrowNotFoundExceptionWhenCustomerDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> customerService.findById(id));
            verifyNoInteractions(customerMapper);
        }
    }

    @Nested
    @DisplayName("inactivate")
    class Inactivate {

        @Test
        @DisplayName("should mark the customer as inactive without calling save() explicitly")
        void shouldInactivateCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id, "guilherme@email.com", true);

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

            customerService.inactivate(id);

            assertFalse(customer.getActive());
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when the customer does not exist")
        void shouldThrowExceptionWhenInactivatingNonExistentCustomer() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> customerService.inactivate(id));
        }
    }
}