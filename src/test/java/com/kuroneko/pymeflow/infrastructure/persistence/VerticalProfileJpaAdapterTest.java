package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = "spring.flyway.enabled=false")
class VerticalProfileJpaAdapterTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private VerticalProfileJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new VerticalProfileJpaAdapter(jdbcTemplate);
        jdbcTemplate.execute("create table vertical_profiles (id varchar(63) primary key, display_name varchar(120) not null, enabled boolean not null)");
        jdbcTemplate.execute("create table vertical_profile_categories (profile_id varchar(63) not null, category_key varchar(80) not null, display_name varchar(120) not null, direction varchar(20) not null, sort_order integer not null)");
        jdbcTemplate.execute("create table vertical_profile_rules (profile_id varchar(63) not null, rule_key varchar(100) not null, condition_key varchar(120) not null, threshold numeric(18, 2) not null, action_key varchar(100) not null)");
        jdbcTemplate.execute("create table vertical_profile_obligation_templates (profile_id varchar(63) not null, obligation_key varchar(100) not null, display_name varchar(120) not null, estimated_amount numeric(18, 2) not null, frequency varchar(20) not null, due_day_of_month integer not null)");
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", "pharmacy-cl", "Farmacia chilena");
        jdbcTemplate.update("insert into vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order) values (?, ?, ?, ?, ?)", "pharmacy-cl", "sales", "Ventas", "INFLOW", 10);
        jdbcTemplate.update("insert into vertical_profile_rules (profile_id, rule_key, condition_key, threshold, action_key) values (?, ?, ?, ?, ?)", "pharmacy-cl", "low-balance-warning", "projected_balance_below_threshold", 250000, "low-balance-warning");
        jdbcTemplate.update("insert into vertical_profile_obligation_templates (profile_id, obligation_key, display_name, estimated_amount, frequency, due_day_of_month) values (?, ?, ?, ?, ?, ?)", "pharmacy-cl", "rent", "Arriendo", 900000, "P1M", 5);
    }

    @Test
    void loadsSeededProfileCategoriesRulesAndObligations() {
        var profile = adapter.loadProfile(new ProfileId("pharmacy-cl"));

        assertThat(profile).isPresent();
        assertThat(profile.orElseThrow().categories()).hasSize(1);
        assertThat(profile.orElseThrow().categories().getFirst().direction()).isEqualTo(CashflowDirection.INFLOW);
        assertThat(profile.orElseThrow().rules()).hasSize(1);
        assertThat(profile.orElseThrow().obligations()).hasSize(1);
    }
}
