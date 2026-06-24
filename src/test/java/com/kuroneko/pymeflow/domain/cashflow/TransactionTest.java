package com.kuroneko.pymeflow.domain.cashflow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final LocalDate BOOKED_AT = LocalDate.of(2026, 6, 19);

    @Test
    void canonicalConstructorStoresExplicitDirection() {
        var transaction = new Transaction("Supplier payment", BigDecimal.valueOf(15_000), CLP, BOOKED_AT, TransactionDirection.DEBIT);

        assertThat(transaction.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(transaction.amount()).isEqualByComparingTo("15000");
    }

    @Test
    void compatibilityConstructorDefaultsDirectionToCredit() {
        var transaction = new Transaction("Sale", BigDecimal.valueOf(20_000), CLP, BOOKED_AT);

        assertThat(transaction.direction()).isEqualTo(TransactionDirection.CREDIT);
    }

    @Test
    void rejectsNullDirection() {
        assertThatThrownBy(() -> new Transaction("Sale", BigDecimal.valueOf(20_000), CLP, BOOKED_AT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Direction is required");
    }

    @Test
    void rejectsMissingCoreFields() {
        assertThatThrownBy(() -> new Transaction(null, BigDecimal.valueOf(20_000), CLP, BOOKED_AT, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description is required");
        assertThatThrownBy(() -> new Transaction(" ", BigDecimal.valueOf(20_000), CLP, BOOKED_AT, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description is required");
        assertThatThrownBy(() -> new Transaction("Sale", null, CLP, BOOKED_AT, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount is required");
        assertThatThrownBy(() -> new Transaction("Sale", BigDecimal.valueOf(20_000), null, BOOKED_AT, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
        assertThatThrownBy(() -> new Transaction("Sale", BigDecimal.valueOf(20_000), CLP, null, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booked date is required");
    }
}
