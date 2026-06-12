package com.kuroneko.pymeflow.infrastructure.export;

import com.kuroneko.pymeflow.application.export.MonthlyCashflowSummary;
import com.kuroneko.pymeflow.application.port.out.AccountantExportPort;
import com.kuroneko.pymeflow.domain.export.ExportPeriod;
import com.kuroneko.pymeflow.domain.tenant.TenantId;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CsvAccountantExportAdapter implements AccountantExportPort {
    private static final String MEDIA_TYPE = "text/csv;charset=UTF-8";

    @Override
    public ExportResult export(MonthlyCashflowSummary summary) {
        var csv = new StringBuilder("section,category_key,label,amount\n");
        appendLines(csv, "inflows", summary.inflows());
        appendLines(csv, "outflows", summary.outflows());
        appendLines(csv, "obligations", summary.obligations());
        appendLines(csv, "settlements", summary.settlements());
        appendTotal(csv, "total_inflows", summary.totalInflows());
        appendTotal(csv, "total_outflows", summary.totalOutflows());
        appendTotal(csv, "total_obligations", summary.totalObligations());
        appendTotal(csv, "total_settlements", summary.totalSettlements());
        appendTotal(csv, "net_total", summary.netTotal());
        return new ExportResult(MEDIA_TYPE, csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ExportResult export(ExportPeriod period, TenantId tenantId) {
        var summary = new MonthlyCashflowSummary(
                tenantId,
                period,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO
        );
        return export(summary);
    }

    private static void appendLines(StringBuilder csv, String section, java.util.List<MonthlyCashflowSummary.ExportLine> lines) {
        for (MonthlyCashflowSummary.ExportLine line : lines) {
            csv.append(escape(section)).append(',')
                    .append(escape(line.categoryKey())).append(',')
                    .append(escape(line.label())).append(',')
                    .append(line.amount()).append('\n');
        }
    }

    private static void appendTotal(StringBuilder csv, String label, java.math.BigDecimal amount) {
        csv.append("summary,,").append(escape(label)).append(',').append(amount).append('\n');
    }

    private static String escape(String value) {
        var text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
