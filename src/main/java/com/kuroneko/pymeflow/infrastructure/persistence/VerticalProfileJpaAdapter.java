package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.port.out.ProfileRegistryPort;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ObligationTemplate;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Period;
import java.util.Optional;

@Repository
public class VerticalProfileJpaAdapter implements ProfileRegistryPort {
    private final JdbcTemplate jdbcTemplate;

    public VerticalProfileJpaAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<VerticalProfile> loadProfile(ProfileId id) {
        var profiles = jdbcTemplate.query(
                "select id, display_name from vertical_profiles where id = ? and enabled = true",
                (rs, rowNum) -> new ProfileRow(rs.getString("id"), rs.getString("display_name")),
                id.value()
        );
        if (profiles.isEmpty()) {
            return Optional.empty();
        }

        var profile = profiles.getFirst();
        return Optional.of(new VerticalProfile(
                new ProfileId(profile.id()),
                profile.displayName(),
                rules(profile.id()),
                categories(profile.id()),
                obligations(profile.id())
        ));
    }

    private java.util.List<CashflowCategory> categories(String profileId) {
        return jdbcTemplate.query(
                "select category_key, display_name, direction from vertical_profile_categories where profile_id = ? order by sort_order",
                (rs, rowNum) -> new CashflowCategory(
                        rs.getString("category_key"),
                        rs.getString("display_name"),
                        CashflowDirection.valueOf(rs.getString("direction"))
                ),
                profileId
        );
    }

    private java.util.List<ProfileRule> rules(String profileId) {
        return jdbcTemplate.query(
                "select rule_key, condition_key, threshold, action_key from vertical_profile_rules where profile_id = ? order by rule_key",
                (rs, rowNum) -> new ProfileRule(
                        rs.getString("rule_key"),
                        rs.getString("condition_key"),
                        rs.getBigDecimal("threshold"),
                        rs.getString("action_key")
                ),
                profileId
        );
    }

    private java.util.List<ObligationTemplate> obligations(String profileId) {
        return jdbcTemplate.query(
                "select obligation_key, display_name, estimated_amount, frequency, due_day_of_month from vertical_profile_obligation_templates where profile_id = ? order by obligation_key",
                (rs, rowNum) -> new ObligationTemplate(
                        rs.getString("obligation_key"),
                        rs.getString("display_name"),
                        rs.getBigDecimal("estimated_amount"),
                        Period.parse(rs.getString("frequency")),
                        rs.getInt("due_day_of_month")
                ),
                profileId
        );
    }

    private record ProfileRow(String id, String displayName) {
    }
}
