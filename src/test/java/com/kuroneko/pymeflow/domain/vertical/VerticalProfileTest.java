package com.kuroneko.pymeflow.domain.vertical;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Period;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalProfileTest {

    @Test
    void createsImmutableProfileWithValidatedCollections() {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        var obligation = new ObligationTemplate("rent", "Arriendo", BigDecimal.TEN, Period.ofMonths(1), 5);
        var rule = new ProfileRule("balance", "below_threshold", BigDecimal.ONE, "notify");

        var profile = new VerticalProfile(new ProfileId("retail-cl"), "Comercio", List.of(rule), List.of(category), List.of(obligation));

        assertThat(profile.categories()).containsExactly(category);
        assertThatThrownBy(() -> profile.categories().add(category)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsProfileWithoutCategories() {
        assertThatThrownBy(() -> new VerticalProfile(new ProfileId("retail-cl"), "Comercio", List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one category");
    }

    @Test
    void validatesProfileRuleRequiredFields() {
        assertThatThrownBy(() -> new ProfileRule(" ", "below_threshold", BigDecimal.ONE, "notify"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rule key");
        assertThatThrownBy(() -> new ProfileRule("balance", "below_threshold", null, "notify"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Threshold");
    }

    @Test
    void validatesProfileIdShape() {
        assertThatThrownBy(() -> new ProfileId("Retail_CL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebab-case");
    }
}
