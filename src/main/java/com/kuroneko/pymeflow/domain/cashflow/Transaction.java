package com.kuroneko.pymeflow.domain.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public record Transaction(String description, BigDecimal amount, Currency currency, LocalDate bookedAt, TransactionDirection direction) {
    public Transaction(String description, BigDecimal amount, Currency currency, LocalDate bookedAt) {
        this(description, amount, currency, bookedAt, TransactionDirection.CREDIT);
    }

    public Transaction {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (bookedAt == null) {
            throw new IllegalArgumentException("Booked date is required");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
    }
}
