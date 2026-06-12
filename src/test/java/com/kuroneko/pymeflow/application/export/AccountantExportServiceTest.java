package com.kuroneko.pymeflow.application.export;

import com.kuroneko.pymeflow.application.port.out.AccountantExportPort;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.export.ExportPeriod;
import com.kuroneko.pymeflow.domain.tenant.TenantId;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kuroneko.pymeflow.application.export.AccountantExportService.CategorizedCashflowLine;
import static com.kuroneko.pymeflow.application.export.AccountantExportService.MonthlyExportCommand;
import static com.kuroneko.pymeflow.application.export.MonthlyCashflowSummary.ExportLine;
import static org.assertj.core.api.Assertions.assertThat;

class AccountantExportServiceTest {

    @Test
    void buildsMonthlySummaryAndDelegatesToExportPort() {
        var exportPort = new CapturingExportPort();
        var service = new AccountantExportService(exportPort);
        var tenantId = new TenantId(UUID.randomUUID());
        var period = new ExportPeriod(YearMonth.of(2026, 6));
        var inflow = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var outflow = new CashflowCategory("rent", "Rent", CashflowDirection.OUTFLOW);
        var obligation = new ExportLine("supplier", "Supplier", BigDecimal.valueOf(30));
        var settlement = new ExportLine("settlement", "Settlement", BigDecimal.valueOf(20));

        var result = service.exportMonthly(new MonthlyExportCommand(
                tenantId,
                period,
                List.of(
                        line(transaction(BigDecimal.valueOf(100)), inflow),
                        line(transaction(BigDecimal.valueOf(-40)), outflow)
                ),
                List.of(obligation),
                List.of(settlement)
        ));

        assertThat(result.mediaType()).isEqualTo("text/csv");
        assertThat(exportPort.summary.totalInflows()).isEqualByComparingTo("100");
        assertThat(exportPort.summary.totalOutflows()).isEqualByComparingTo("40");
        assertThat(exportPort.summary.totalObligations()).isEqualByComparingTo("30");
        assertThat(exportPort.summary.totalSettlements()).isEqualByComparingTo("20");
        assertThat(exportPort.summary.netTotal()).isEqualByComparingTo("50");
    }

    private static CategorizedCashflowLine line(Transaction transaction, CashflowCategory category) {
        return new CategorizedCashflowLine(transaction, new CategoryAssignment(Optional.of(category), false));
    }

    private static Transaction transaction(BigDecimal amount) {
        return new Transaction("Cashflow", amount, Currency.getInstance("CLP"), LocalDate.now());
    }

    private static final class CapturingExportPort implements AccountantExportPort {
        private MonthlyCashflowSummary summary;

        @Override
        public ExportResult export(MonthlyCashflowSummary summary) {
            this.summary = summary;
            return new ExportResult("text/csv", new byte[]{1});
        }

        @Override
        public ExportResult export(ExportPeriod period, TenantId tenantId) {
            throw new UnsupportedOperationException("Use summary export");
        }
    }
}
