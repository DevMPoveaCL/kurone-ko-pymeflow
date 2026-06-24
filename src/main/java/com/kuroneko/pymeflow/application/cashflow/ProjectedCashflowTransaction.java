package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public record ProjectedCashflowTransaction(
        String categoryKey,
        BigDecimal amount,
        Currency currency,
        LocalDate date,
        TransactionDirection direction
) {
    public ProjectedCashflowTransaction(String categoryKey, BigDecimal amount, Currency currency, LocalDate date) {
        this(categoryKey, amount, currency, date, TransactionDirection.CREDIT);
    }

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
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
    }
}
