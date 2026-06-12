package com.kuroneko.pymeflow.infrastructure.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileDrivenCashflowCategorizationAdapterTest {

    private final ProfileDrivenCashflowCategorizationAdapter adapter = new ProfileDrivenCashflowCategorizationAdapter();

    @Test
    void categorizesTransactionFromProfileCategoryDisplayName() {
        var sales = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(new ProfileId("retail-cl"), "Retail", List.of(), List.of(sales), List.of());

        var assignment = adapter.categorize(transaction("Venta Caja 1"), profile);

        assertThat(assignment.category()).contains(sales);
        assertThat(assignment.requiresManualReview()).isFalse();
    }

    @Test
    void categorizesTransactionFromProfileRuleThatPointsToCategory() {
        var sales = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        var rule = new ProfileRule("cash-sales", "description_contains", BigDecimal.ZERO, "sales");
        var profile = new VerticalProfile(new ProfileId("retail-cl"), "Retail", List.of(rule), List.of(sales), List.of());

        var assignment = adapter.categorize(transaction("Venta Caja 1 cash"), profile);

        assertThat(assignment.category()).contains(sales);
        assertThat(assignment.requiresManualReview()).isFalse();
    }

    @Test
    void fallsBackToManualReviewWhenNoProfileCategoryMatches() {
        var sales = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(new ProfileId("retail-cl"), "Retail", List.of(), List.of(sales), List.of());

        var assignment = adapter.categorize(transaction("Movimiento sin clasificacion"), profile);

        assertThat(assignment.category()).isEmpty();
        assertThat(assignment.requiresManualReview()).isTrue();
    }

    private static Transaction transaction(String description) {
        return new Transaction(description, BigDecimal.valueOf(1000), Currency.getInstance("CLP"), LocalDate.now());
    }
}
