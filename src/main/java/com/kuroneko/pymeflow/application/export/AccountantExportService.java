package com.kuroneko.pymeflow.application.export;

import com.kuroneko.pymeflow.application.port.out.AccountantExportPort;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.export.ExportPeriod;
import com.kuroneko.pymeflow.domain.tenant.TenantId;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.kuroneko.pymeflow.application.export.MonthlyCashflowSummary.ExportLine;

public final class AccountantExportService {
    private final AccountantExportPort accountantExportPort;

    public AccountantExportService(AccountantExportPort accountantExportPort) {
        this.accountantExportPort = accountantExportPort;
    }

    public AccountantExportPort.ExportResult exportMonthly(MonthlyExportCommand command) {
        return accountantExportPort.export(buildSummary(command));
    }

    public MonthlyCashflowSummary buildSummary(MonthlyExportCommand command) {
        var inflows = new ArrayList<ExportLine>();
        var outflows = new ArrayList<ExportLine>();

        for (CategorizedCashflowLine line : command.cashflowLines()) {
            line.assignment().category().ifPresent(category -> {
                var exportLine = new ExportLine(category.key(), category.displayName(), line.transaction().amount().abs());
                if (category.direction() == CashflowDirection.INFLOW) {
                    inflows.add(exportLine);
                } else if (category.direction() == CashflowDirection.OUTFLOW) {
                    outflows.add(exportLine);
                }
            });
        }

        var totalInflows = total(inflows);
        var totalOutflows = total(outflows);
        var totalObligations = total(command.obligations());
        var totalSettlements = total(command.settlements());
        var netTotal = totalInflows.add(totalSettlements).subtract(totalOutflows).subtract(totalObligations);

        return new MonthlyCashflowSummary(
                command.tenantId(),
                command.period(),
                inflows,
                outflows,
                command.obligations(),
                command.settlements(),
                totalInflows,
                totalOutflows,
                totalObligations,
                totalSettlements,
                netTotal
        );
    }

    private static BigDecimal total(List<ExportLine> lines) {
        return lines.stream()
                .map(line -> line.amount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record MonthlyExportCommand(
            TenantId tenantId,
            ExportPeriod period,
            List<CategorizedCashflowLine> cashflowLines,
            List<ExportLine> obligations,
            List<ExportLine> settlements
    ) {
        public MonthlyExportCommand {
            if (tenantId == null) {
                throw new IllegalArgumentException("Tenant id is required");
            }
            if (period == null) {
                throw new IllegalArgumentException("Export period is required");
            }
            cashflowLines = List.copyOf(cashflowLines == null ? List.of() : cashflowLines);
            obligations = List.copyOf(obligations == null ? List.of() : obligations);
            settlements = List.copyOf(settlements == null ? List.of() : settlements);
        }
    }

    public record CategorizedCashflowLine(Transaction transaction, CategoryAssignment assignment) {
        public CategorizedCashflowLine {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (assignment == null) {
                throw new IllegalArgumentException("Category assignment is required");
            }
        }
    }
}
