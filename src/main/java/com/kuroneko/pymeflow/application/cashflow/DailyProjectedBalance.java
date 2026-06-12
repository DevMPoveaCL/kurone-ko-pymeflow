package com.kuroneko.pymeflow.application.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyProjectedBalance(
        LocalDate date,
        BigDecimal inflows,
        BigDecimal outflows,
        BigDecimal obligations,
        BigDecimal balance
) {
    public DailyProjectedBalance {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        inflows = requireAmount(inflows, "Inflows are required");
        outflows = requireAmount(outflows, "Outflows are required");
        obligations = requireAmount(obligations, "Obligations are required");
        balance = requireAmount(balance, "Balance is required");
    }

    private static BigDecimal requireAmount(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
