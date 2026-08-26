package com.godoy.billing.domain.enums;

import java.time.LocalDate;

public enum BillingCycle {

    MONTHLY {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusMonths(1);
        }
    },
    YEARLY {
        @Override
        public LocalDate advance(LocalDate from) {
            return from.plusYears(1);
        }
    };

    public abstract LocalDate advance(LocalDate from);
}
