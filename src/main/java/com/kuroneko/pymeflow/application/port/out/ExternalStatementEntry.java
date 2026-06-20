package com.kuroneko.pymeflow.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public record ExternalStatementEntry(
        String externalReference,
        LocalDate date,
        String description,
        BigDecimal amount,
        Currency currency,
        String counterpartyName,
        String accountAlias
) {
    public ExternalStatementEntry(
            String externalReference,
            LocalDate date,
            String description,
            BigDecimal amount,
            Currency currency
    ) {
        this(externalReference, date, description, amount, currency, null, null);
    }

    public ExternalStatementEntry {
        if (externalReference == null || externalReference.isBlank()) {
            throw new IllegalArgumentException("External reference is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount must be non-zero");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
    }
}
