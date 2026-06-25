package com.kuroneko.pymeflow.infrastructure.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("providerSyncStorage")
public final class ProviderSyncStorageHealthIndicator implements HealthIndicator {
    private static final String STORAGE_TABLE = "provider_sync_sessions";
    private static final String CAPABILITY = "durable-provider-sync-sessions";
    private static final String SCOPE = "storage-only";
    private static final String STORAGE_UNREACHABLE = "PROVIDER_SYNC_STORAGE_UNREACHABLE";

    private final JdbcTemplate jdbcTemplate;

    public ProviderSyncStorageHealthIndicator(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("JdbcTemplate is required");
        }
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            jdbcTemplate.queryForObject("select count(*) from provider_sync_sessions", Long.class);
            return withStorageDetails(Health.up(), "reachable").build();
        } catch (DataAccessException exception) {
            return withStorageDetails(Health.down(), "unreachable")
                    .withDetail("errorCode", STORAGE_UNREACHABLE)
                    .build();
        }
    }

    private static Health.Builder withStorageDetails(Health.Builder builder, String reachability) {
        return builder
                .withDetail("scope", SCOPE)
                .withDetail("storage", STORAGE_TABLE)
                .withDetail("storageReachability", reachability)
                .withDetail("capability", CAPABILITY);
    }
}
