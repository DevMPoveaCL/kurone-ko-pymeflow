package com.kuroneko.pymeflow.infrastructure.demo;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:demo-data-reset;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcDemoDataAdapterTest {
    private static final ProfileId DEMO_PROFILE = new ProfileId("pharmacy-cl");
    private static final ProfileId OTHER_PROFILE = new ProfileId("other-profile");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JdbcDemoDataAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new JdbcDemoDataAdapter(jdbcTemplate);
        jdbcTemplate.execute("drop table if exists cockpit_preferences");
        jdbcTemplate.execute("drop table if exists provider_sync_sessions");
        jdbcTemplate.execute("drop table if exists cashflow_movement_history");
        jdbcTemplate.execute("drop table if exists vertical_profile_categories");
        jdbcTemplate.execute("drop table if exists vertical_profile_rules");
        jdbcTemplate.execute("drop table if exists vertical_profile_obligation_templates");
        jdbcTemplate.execute("drop table if exists vertical_profiles");
        jdbcTemplate.execute("create table vertical_profiles (id varchar(63) primary key, display_name varchar(120) not null, enabled boolean not null default true, created_at timestamp with time zone not null default now())");
        jdbcTemplate.execute("create table vertical_profile_categories (profile_id varchar(63) not null references vertical_profiles(id), category_key varchar(80) not null, display_name varchar(120) not null, direction varchar(20) not null, sort_order integer not null, primary key (profile_id, category_key))");
        jdbcTemplate.execute("create table vertical_profile_rules (profile_id varchar(63) not null references vertical_profiles(id), rule_key varchar(100) not null, condition_key varchar(120) not null, threshold numeric(18, 2) not null, action_key varchar(100) not null, primary key (profile_id, rule_key))");
        jdbcTemplate.execute("create table vertical_profile_obligation_templates (profile_id varchar(63) not null references vertical_profiles(id), obligation_key varchar(100) not null, display_name varchar(120) not null, estimated_amount numeric(18, 2) not null, frequency varchar(20) not null, due_day_of_month integer not null, primary key (profile_id, obligation_key))");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V2__create_cashflow_movement_history.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__add_movement_direction.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V5__create_provider_sync_sessions.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V6__create_cockpit_preferences.sql"));
        }
        insertProfile(DEMO_PROFILE, "Demo profile");
        insertProfile(OTHER_PROFILE, "Other profile");
        insertCategory(DEMO_PROFILE, "sales");
        insertCategory(OTHER_PROFILE, "sales");
        insertDemoRows(DEMO_PROFILE, "demo");
        insertDemoRows(OTHER_PROFILE, "other");
    }

    @Test
    void resetDeletesOnlyProfileScopedTransactionalRows() {
        adapter.reset(DEMO_PROFILE);

        assertThat(count("cashflow_movement_history", DEMO_PROFILE)).isZero();
        assertThat(count("provider_sync_sessions", DEMO_PROFILE)).isZero();
        assertThat(count("cockpit_preferences", DEMO_PROFILE)).isZero();

        assertThat(count("cashflow_movement_history", OTHER_PROFILE)).isEqualTo(1);
        assertThat(count("provider_sync_sessions", OTHER_PROFILE)).isEqualTo(1);
        assertThat(count("cockpit_preferences", OTHER_PROFILE)).isEqualTo(1);
    }

    @Test
    void resetLeavesReferenceTablesAndOtherProfilesUntouched() {
        adapter.reset(DEMO_PROFILE);

        assertThat(jdbcTemplate.queryForObject("select count(*) from vertical_profiles", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from vertical_profile_categories", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from vertical_profiles where id = ?", Integer.class, DEMO_PROFILE.value())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from vertical_profiles where id = ?", Integer.class, OTHER_PROFILE.value())).isEqualTo(1);
    }

    private void insertProfile(ProfileId profileId, String displayName) {
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", profileId.value(), displayName);
    }

    private void insertCategory(ProfileId profileId, String categoryKey) {
        jdbcTemplate.update("insert into vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order) values (?, ?, ?, ?, ?)",
                profileId.value(), categoryKey, categoryKey, "INFLOW", 10);
    }

    private void insertDemoRows(ProfileId profileId, String prefix) {
        jdbcTemplate.update("""
                        insert into cashflow_movement_history
                        (id, profile_id, amount, currency, movement_date, movement_direction, status, category_key, safe_description, source_reference, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                profileId.value(),
                1200,
                "CLP",
                LocalDate.of(2026, 6, 1),
                "CREDIT",
                "PROJECTABLE",
                "sales",
                prefix + " movement",
                prefix + "-source-001",
                Instant.parse("2026-06-20T10:00:00Z"),
                Instant.parse("2026-06-20T10:00:00Z")
        );
        jdbcTemplate.update("""
                        insert into provider_sync_sessions
                        (sync_id, profile_id, provider_type, status, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                "sync-" + prefix,
                profileId.value(),
                "fixture-demo",
                "COMPLETED",
                Instant.parse("2026-06-20T10:00:00Z"),
                Instant.parse("2026-06-20T10:00:00Z")
        );
        jdbcTemplate.update("""
                        insert into cockpit_preferences
                        (profile_id, opening_balance, preferred_horizon_days, created_at, updated_at)
                        values (?, ?, ?, ?, ?)
                        """,
                profileId.value(),
                350000,
                7,
                Instant.parse("2026-06-20T10:00:00Z"),
                Instant.parse("2026-06-20T10:00:00Z")
        );
    }

    private int count(String table, ProfileId profileId) {
        return jdbcTemplate.queryForObject("select count(*) from " + table + " where profile_id = ?", Integer.class, profileId.value());
    }
}
