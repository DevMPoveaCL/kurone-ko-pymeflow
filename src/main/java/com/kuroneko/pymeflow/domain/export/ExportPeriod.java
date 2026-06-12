package com.kuroneko.pymeflow.domain.export;

import java.time.YearMonth;

public record ExportPeriod(YearMonth value) {
    public ExportPeriod {
        if (value == null) {
            throw new IllegalArgumentException("Export period is required");
        }
    }
}
