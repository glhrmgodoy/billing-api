package com.godoy.billing.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entityName, Object id) {
        return new NotFoundException("%s não encontrado com ID: %s".formatted(entityName, id));
    }
}
