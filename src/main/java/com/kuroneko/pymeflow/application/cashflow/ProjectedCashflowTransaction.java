package com.kuroneko.pymeflow.application.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public record ProjectedCashflowTransaction(
        String categoryKey,
        BigDecimal amount,
        Currency currency,
        LocalDate date
) {
    public ProjectedCashflowTransaction {
        if (categoryKey == null || categoryKey.isBlank()) {
            throw new IllegalArgumentException("Category key is required");
        }
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be zero or positive");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
    }
}
