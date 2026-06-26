package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public final class CockpitProjectionService {
    public static final int MAX_HORIZON_DAYS = 90;
    private static final Currency CLP = Currency.getInstance("CLP");

    private final CashflowMovementHistoryService historyService;
    private final CashflowProjectionService projectionService;

    public CockpitProjectionService(
            CashflowMovementHistoryService historyService,
            CashflowProjectionService projectionService
    ) {
        this.historyService = historyService;
        this.projectionService = projectionService;
    }

    public CashflowProjectionResult projectFromHistory(
            ProfileId profileId,
            BigDecimal openingBalance,
            LocalDate startDate,
            int horizonDays
    ) {
        validate(openingBalance, startDate, horizonDays);
        var endDate = startDate.plusDays(horizonDays - 1L);
        var transactions = historyService.projectionReady(profileId, startDate, endDate).stream()
                .map(ProjectionReadyCashflowTransaction::toProjectionTransaction)
                .toList();

        if (transactions.isEmpty()) {
            return new CashflowProjectionResult(List.of(), openingBalance, List.of(), List.of());
        }

        return projectionService.project(new CashflowProjectionCommand(
                profileId,
                openingBalance,
                CLP,
                startDate,
                horizonDays,
                transactions
        ));
    }

    private static void validate(BigDecimal openingBalance, LocalDate startDate, int horizonDays) {
        if (openingBalance == null) {
            throw new IllegalArgumentException("El saldo inicial es obligatorio.");
        }
        if (openingBalance.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        }
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("El horizonte debe ser mayor que cero.");
        }
        if (horizonDays > MAX_HORIZON_DAYS) {
            throw new IllegalArgumentException("El horizonte no puede superar 90 días.");
        }
    }
}
