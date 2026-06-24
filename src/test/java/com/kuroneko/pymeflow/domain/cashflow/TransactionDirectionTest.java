package com.kuroneko.pymeflow.domain.cashflow;

import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionDirectionTest {

    @Test
    void definesDebitAndCreditOnly() {
        assertThat(TransactionDirection.values())
                .extracting(Enum::name)
                .containsExactly("DEBIT", "CREDIT");
    }

    @Test
    void remainsSeparateFromCashflowCategoryDirection() {
        assertThat(TransactionDirection.class).isNotEqualTo(CashflowDirection.class);
        assertThat(TransactionDirection.values())
                .extracting(Enum::name)
                .doesNotContain("INFLOW", "OUTFLOW", "TRANSFER");
        assertThat(CashflowDirection.values())
                .extracting(Enum::name)
                .containsExactly("INFLOW", "OUTFLOW", "TRANSFER");
    }
}
