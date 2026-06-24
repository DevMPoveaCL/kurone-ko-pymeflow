package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalStatementEntryTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 19);

    @Test
    void acceptsSignedAmountsUnchangedWithExplicitDirection() {
        var entry = new ExternalStatementEntry(
                "bank-1",
                DATE,
                "Supplier payment",
                BigDecimal.valueOf(-15_000),
                CLP,
                TransactionDirection.DEBIT,
                "Supplier",
                "Checking"
        );

        assertThat(entry.amount()).isEqualByComparingTo("-15000");
        assertThat(entry.direction()).isEqualTo(TransactionDirection.DEBIT);
    }

    @Test
    void compatibilityConstructorLeavesDirectionUnsetForAdapterMapping() {
        var entry = new ExternalStatementEntry("bank-2", DATE, "Sale", BigDecimal.valueOf(20_000), CLP);

        assertThat(entry.amount()).isEqualByComparingTo("20000");
        assertThat(entry.direction()).isNull();
    }
}
