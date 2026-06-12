package com.kuroneko.pymeflow.application.vertical;

import com.kuroneko.pymeflow.application.port.out.ProfileRegistryPort;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalProfileServiceTest {

    @Test
    void loadsProfileAndExposesActiveRules() {
        var profileId = new ProfileId("retail-cl");
        var rule = new ProfileRule("low-balance", "projected_balance_below_threshold", BigDecimal.TEN, "warn");
        var service = new VerticalProfileService(registry(profile(profileId, List.of(rule))));

        assertThat(service.activeRules(profileId)).containsExactly(rule);
    }

    @Test
    void rejectsDuplicateRuleKeys() {
        var profileId = new ProfileId("retail-cl");
        var rules = List.of(
                new ProfileRule("same", "projected_balance_below_threshold", BigDecimal.TEN, "first"),
                new ProfileRule("same", "projected_balance_above_threshold", BigDecimal.ONE, "second")
        );
        var service = new VerticalProfileService(registry(profile(profileId, rules)));

        assertThatThrownBy(() -> service.loadProfile(profileId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate rule key");
    }

    private static ProfileRegistryPort registry(VerticalProfile profile) {
        return id -> profile.id().equals(id) ? Optional.of(profile) : Optional.empty();
    }

    private static VerticalProfile profile(ProfileId profileId, List<ProfileRule> rules) {
        return new VerticalProfile(
                profileId,
                "Retail",
                rules,
                List.of(new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW)),
                List.of()
        );
    }
}
