package com.kuroneko.pymeflow.application.export;

import com.kuroneko.pymeflow.domain.export.ExportPeriod;
import com.kuroneko.pymeflow.domain.tenant.TenantId;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyCashflowSummary(
        TenantId tenantId,
        ExportPeriod period,
        List<ExportLine> inflows,
        List<ExportLine> outflows,
        List<ExportLine> obligations,
        List<ExportLine> settlements,
        BigDecimal totalInflows,
        BigDecimal totalOutflows,
        BigDecimal totalObligations,
        BigDecimal totalSettlements,
        BigDecimal netTotal
) {
    public MonthlyCashflowSummary {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required");
        }
        if (period == null) {
            throw new IllegalArgumentException("Export period is required");
        }
        inflows = List.copyOf(inflows == null ? List.of() : inflows);
        outflows = List.copyOf(outflows == null ? List.of() : outflows);
        obligations = List.copyOf(obligations == null ? List.of() : obligations);
        settlements = List.copyOf(settlements == null ? List.of() : settlements);
        totalInflows = requireAmount(totalInflows, "Total inflows are required");
        totalOutflows = requireAmount(totalOutflows, "Total outflows are required");
        totalObligations = requireAmount(totalObligations, "Total obligations are required");
        totalSettlements = requireAmount(totalSettlements, "Total settlements are required");
        netTotal = requireAmount(netTotal, "Net total is required");
    }

    private static BigDecimal requireAmount(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public record ExportLine(String categoryKey, String label, BigDecimal amount) {
        public ExportLine {
            if (categoryKey == null || categoryKey.isBlank()) {
                throw new IllegalArgumentException("Category key is required");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Label is required");
            }
            if (amount == null) {
                throw new IllegalArgumentException("Amount is required");
            }
        }
    }
}
