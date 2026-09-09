package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.dto.request.LoginRequest;
import com.godoy.billing.dto.response.AuthResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.repository.CustomerRepository;
import com.godoy.billing.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Customer buildCustomer(boolean active) {
        return Customer.builder()
                .id(UUID.randomUUID())
                .name("Guilherme")
                .email("guilherme@email.com")
                .password("stored-hash")
                .active(active)
                .build();
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("deve gerar o token quando as credenciais forem válidas")
        void shouldLoginSuccessfully() {
            Customer customer = buildCustomer(true);
            LoginRequest request = new LoginRequest(customer.getEmail(), "correct-password");

            when(customerRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("correct-password", customer.getPassword())).thenReturn(true);
            when(jwtService.generateToken(customer.getEmail())).thenReturn("generated-token");
            when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

            AuthResponse result = authService.login(request);

            assertThat(result.token()).isEqualTo("generated-token");
            assertThat(result.tokenType()).isEqualTo("Bearer");
            assertThat(result.expiresInMs()).isEqualTo(3_600_000L);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando o e-mail não existir")
        void shouldThrowExceptionWhenEmailDoesNotExist() {
            LoginRequest request = new LoginRequest("doesnotexist@email.com", "whatever");
            when(customerRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
            verifyNoInteractions(passwordEncoder, jwtService);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando a conta estiver inativa")
        void shouldThrowExceptionWhenAccountIsInactive() {
            Customer customer = buildCustomer(false);
            LoginRequest request = new LoginRequest(customer.getEmail(), "correct-password");

            when(customerRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(passwordEncoder, jwtService);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando a senha estiver incorreta")
        void shouldThrowExceptionWhenPasswordIsIncorrect() {
            Customer customer = buildCustomer(true);
            LoginRequest request = new LoginRequest(customer.getEmail(), "wrong-password");

            when(customerRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrong-password", customer.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
            verifyNoInteractions(jwtService);
        }
    }
}