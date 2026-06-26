package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
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
import java.math.BigDecimal;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cockpit-preferences;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JdbcCockpitPreferencesAdapterTest {
    private static final ProfileId PROFILE_ID = new ProfileId("test-retail-cl");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JdbcCockpitPreferencesAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new JdbcCockpitPreferencesAdapter(jdbcTemplate);
        jdbcTemplate.execute("drop table if exists cockpit_preferences");
        jdbcTemplate.execute("drop table if exists vertical_profiles");
        jdbcTemplate.execute("create table vertical_profiles (id varchar(63) primary key, display_name varchar(120) not null, enabled boolean not null default true, created_at timestamp with time zone not null default now())");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V6__create_cockpit_preferences.sql"));
        }
        jdbcTemplate.update("delete from cockpit_preferences");
        jdbcTemplate.update("delete from vertical_profiles");
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", PROFILE_ID.value(), "Comercio prueba");
    }

    @Test
    void saveThenLoadRoundtripForProfilePreferences() {
        adapter.save(PROFILE_ID, new CockpitPreferences(BigDecimal.valueOf(1_500_000), 30));

        var loaded = adapter.findByProfile(PROFILE_ID);

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().openingBalance()).isEqualByComparingTo("1500000");
        assertThat(loaded.orElseThrow().preferredHorizonDays()).isEqualTo(30);
    }

    @Test
    void saveIsIdempotentAndUpdatesExistingProfileRow() {
        adapter.save(PROFILE_ID, new CockpitPreferences(BigDecimal.valueOf(100_000), 7));
        adapter.save(PROFILE_ID, new CockpitPreferences(BigDecimal.valueOf(250_000), 30));

        var loaded = adapter.findByProfile(PROFILE_ID);
        var rowCount = jdbcTemplate.queryForObject("select count(*) from cockpit_preferences where profile_id = ?", Integer.class, PROFILE_ID.value());

        assertThat(rowCount).isEqualTo(1);
        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().openingBalance()).isEqualByComparingTo("250000");
        assertThat(loaded.orElseThrow().preferredHorizonDays()).isEqualTo(30);
    }

    @Test
    void findByProfileReturnsEmptyWhenNoPreferencesExist() {
        var missing = adapter.findByProfile(PROFILE_ID);

        assertThat(missing).isEmpty();
    }
}
