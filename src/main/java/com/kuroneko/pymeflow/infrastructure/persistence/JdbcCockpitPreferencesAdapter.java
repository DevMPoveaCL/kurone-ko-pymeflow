package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.port.out.CockpitPreferencesPort;
import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

public class JdbcCockpitPreferencesAdapter implements CockpitPreferencesPort {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCockpitPreferencesAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CockpitPreferences> findByProfile(ProfileId profileId) {
        var rows = jdbcTemplate.query(
                """
                        select opening_balance, preferred_horizon_days
                        from cockpit_preferences
                        where profile_id = ?
                        """,
                (rs, rowNum) -> new CockpitPreferences(
                        rs.getBigDecimal("opening_balance"),
                        rs.getInt("preferred_horizon_days")
                ),
                profileId.value()
        );
        return rows.stream().findFirst();
    }

    @Override
    public void save(ProfileId profileId, CockpitPreferences preferences) {
        if (isH2()) {
            saveWithH2Merge(profileId, preferences);
            return;
        }
        jdbcTemplate.update(
                """
                        insert into cockpit_preferences
                        (profile_id, opening_balance, preferred_horizon_days, created_at, updated_at)
                        values (?, ?, ?, now(), now())
                        on conflict (profile_id) do update
                        set opening_balance = excluded.opening_balance,
                            preferred_horizon_days = excluded.preferred_horizon_days,
                            updated_at = now()
                        """,
                profileId.value(),
                preferences.openingBalance(),
                preferences.preferredHorizonDays()
        );
    }

    private void saveWithH2Merge(ProfileId profileId, CockpitPreferences preferences) {
        jdbcTemplate.update(
                """
                        merge into cockpit_preferences
                        (profile_id, opening_balance, preferred_horizon_days, created_at, updated_at)
                        key (profile_id)
                        values (?, ?, ?, now(), now())
                        """,
                profileId.value(),
                preferences.openingBalance(),
                preferences.preferredHorizonDays()
        );
    }

    private boolean isH2() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")));
    }
}
