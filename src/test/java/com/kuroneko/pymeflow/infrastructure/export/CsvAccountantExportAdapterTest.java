package com.kuroneko.pymeflow.infrastructure.export;

import com.kuroneko.pymeflow.application.export.MonthlyCashflowSummary;
import com.kuroneko.pymeflow.domain.export.ExportPeriod;
import com.kuroneko.pymeflow.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static com.kuroneko.pymeflow.application.export.MonthlyCashflowSummary.ExportLine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvAccountantExportAdapterTest {

    @Test
    void formatsMonthlySummaryAsCsv() {
        var adapter = new CsvAccountantExportAdapter();
        var summary = new MonthlyCashflowSummary(
                new TenantId(UUID.randomUUID()),
                new ExportPeriod(YearMonth.of(2026, 6)),
                List.of(new ExportLine("sales", "Ventas", BigDecimal.valueOf(1000))),
                List.of(new ExportLine("rent", "Arriendo", BigDecimal.valueOf(300))),
                List.of(),
                List.of(new ExportLine("settlement", "Abono", BigDecimal.valueOf(500))),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(300),
                BigDecimal.ZERO,
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(1200)
        );

        var result = adapter.export(summary);

        assertThat(result.mediaType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(new String(result.content(), StandardCharsets.UTF_8))
                .contains("section,category_key,label,amount")
                .contains("inflows,sales,Ventas,1000")
                .contains("summary,,net_total,1200");
    }

    @Test
    void exportsRequiredCsvFieldsAndEscapesLabels() {
        var adapter = new CsvAccountantExportAdapter();
        var summary = new MonthlyCashflowSummary(
                new TenantId(UUID.randomUUID()),
                new ExportPeriod(YearMonth.of(2026, 6)),
                List.of(new ExportLine("sales", "Ventas, local", BigDecimal.valueOf(1000))),
                List.of(),
                List.of(),
                List.of(),
                BigDecimal.valueOf(1000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000)
        );

        var csv = new String(adapter.export(summary).content(), StandardCharsets.UTF_8);

        assertThat(csv.lines().findFirst()).contains("section,category_key,label,amount");
        assertThat(csv).contains("inflows,sales,\"Ventas, local\",1000");
        assertThat(csv).contains("summary,,total_inflows,1000");
        assertThat(csv).contains("summary,,net_total,1000");
    }

    @Test
    void exportsEmptyPeriodWithZeroTotals() {
        var adapter = new CsvAccountantExportAdapter();

        var result = adapter.export(
                new ExportPeriod(YearMonth.of(2026, 7)),
                new TenantId(UUID.randomUUID())
        );

        var csv = new String(result.content(), StandardCharsets.UTF_8);
        assertThat(result.mediaType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(csv.lines().findFirst()).contains("section,category_key,label,amount");
        assertThat(csv.lines())
                .noneMatch(line -> line.startsWith("inflows,"))
                .noneMatch(line -> line.startsWith("outflows,"))
                .noneMatch(line -> line.startsWith("obligations,"))
                .noneMatch(line -> line.startsWith("settlements,"));
        assertThat(csv)
                .contains("summary,,total_inflows,0")
                .contains("summary,,total_outflows,0")
                .contains("summary,,total_obligations,0")
                .contains("summary,,total_settlements,0")
                .contains("summary,,net_total,0");
    }

    @Test
    void rejectsInvalidExportPeriodBeforeEmptyExport() {
        assertThatThrownBy(() -> new ExportPeriod(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Export period is required");
    }
}
