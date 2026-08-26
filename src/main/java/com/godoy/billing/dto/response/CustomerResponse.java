package com.godoy.billing.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        Boolean active,
        LocalDateTime createdAt
) {
}
