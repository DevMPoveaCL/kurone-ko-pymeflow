package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public record CashflowProjectionCommand(
        ProfileId profileId,
        BigDecimal openingBalance,
        Currency currency,
        LocalDate startDate,
        int horizonDays,
        List<ProjectedCashflowTransaction> transactions
) {
    public CashflowProjectionCommand {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (openingBalance == null) {
            throw new IllegalArgumentException("Opening balance is required");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("Horizon days must be positive");
        }
        transactions = List.copyOf(transactions == null ? List.of() : transactions);
    }
}
