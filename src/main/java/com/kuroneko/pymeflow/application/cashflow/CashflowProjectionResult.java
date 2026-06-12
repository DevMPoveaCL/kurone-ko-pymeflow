package com.kuroneko.pymeflow.application.cashflow;

import java.math.BigDecimal;
import java.util.List;

public record CashflowProjectionResult(
        List<DailyProjectedBalance> dailyBalances,
        BigDecimal closingProjectedBalance,
        List<AppliedObligation> appliedObligations,
        List<ProjectionAlert> alerts
) {
    public CashflowProjectionResult {
        dailyBalances = List.copyOf(dailyBalances == null ? List.of() : dailyBalances);
        if (closingProjectedBalance == null) {
            throw new IllegalArgumentException("Closing projected balance is required");
        }
        appliedObligations = List.copyOf(appliedObligations == null ? List.of() : appliedObligations);
        alerts = List.copyOf(alerts == null ? List.of() : alerts);
    }
}
