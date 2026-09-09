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

import static org.assertj.core.api.Assertions.*;
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
        @DisplayName("deve criptografar a senha antes de salvar")
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

            assertThat(result).isEqualTo(response);
            assertThat(customerWithoutPassword.getPassword()).isEqualTo("encoded-password");
            verify(passwordEncoder).encode("password12345");
        }

        @Test
        @DisplayName("deve lançar BusinessException quando o e-mail já estiver cadastrado")
        void shouldThrowExceptionForDuplicateEmail() {
            CustomerRequest request = buildRequest("guilherme@email.com");
            when(customerRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> customerService.register(request))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(passwordEncoder, customerMapper);
            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar o cliente quando o id existir")
        void shouldReturnExistingCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id, "guilherme@email.com", true);
            CustomerResponse response = new CustomerResponse(id, "Guilherme", "guilherme@email.com", true, LocalDateTime.now());

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
            when(customerMapper.toResponse(customer)).thenReturn(response);

            assertThat(customerService.findById(id)).isEqualTo(response);
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o id não existir")
        void shouldThrowNotFoundExceptionWhenCustomerDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findById(id))
                    .isInstanceOf(NotFoundException.class);
            verifyNoInteractions(customerMapper);
        }
    }

    @Nested
    @DisplayName("inactivate")
    class Inactivate {

        @Test
        @DisplayName("deve marcar o cliente como inativo sem chamar save() explicitamente")
        void shouldInactivateCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = buildCustomer(id, "guilherme@email.com", true);

            when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

            customerService.inactivate(id);

            assertThat(customer.getActive()).isFalse();
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar NotFoundException quando o cliente não existir")
        void shouldThrowExceptionWhenInactivatingNonExistentCustomer() {
            UUID id = UUID.randomUUID();
            when(customerRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.inactivate(id))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}