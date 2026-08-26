package com.godoy.billing.service;

import com.godoy.billing.domain.entity.Customer;
import com.godoy.billing.dto.request.LoginRequest;
import com.godoy.billing.dto.response.AuthResponse;
import com.godoy.billing.exception.BusinessException;
import com.godoy.billing.repository.CustomerRepository;
import com.godoy.billing.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Email ou senha inválidos"));

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BusinessException("Conta inativa");
        }

        if (!passwordEncoder.matches(request.password(), customer.getPassword())) {
            throw new BusinessException("E-mail ou senha inválidos");
        }

        String token = jwtService.generateToken(customer.getEmail());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs());
    }
}
