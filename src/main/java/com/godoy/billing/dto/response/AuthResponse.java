package com.godoy.billing.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs
) {
}
