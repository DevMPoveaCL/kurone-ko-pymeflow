package com.kuroneko.pymeflow.infrastructure.demo;

import com.kuroneko.pymeflow.application.port.out.DemoDataPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDemoDataAdapter implements DemoDataPort {
    private final JdbcTemplate jdbcTemplate;

    public JdbcDemoDataAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void reset(ProfileId profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        jdbcTemplate.update("delete from cashflow_movement_history where profile_id = ?", profileId.value());
        jdbcTemplate.update("delete from provider_sync_sessions where profile_id = ?", profileId.value());
        jdbcTemplate.update("delete from cockpit_preferences where profile_id = ?", profileId.value());
    }
}
