package com.kuroneko.pymeflow.infrastructure.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSyncStorageHealthIndicatorTest {

    @Test
    void reportsUpWhenDurableProviderSyncSessionStorageIsReachable() {
        var indicator = new ProviderSyncStorageHealthIndicator(migratedJdbcTemplate("provider_sync_health_up"));

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("scope", "storage-only")
                .containsEntry("storage", "provider_sync_sessions")
                .containsEntry("storageReachability", "reachable")
                .containsEntry("capability", "durable-provider-sync-sessions")
                .doesNotContainKeys("bankConnectivity", "providerConnectivity", "networkConnectivity", "credentialStatus");
        assertThat(health.getDetails().toString())
                .doesNotContain("sandbox", "production", "credentialRef", "cursor", "rawPayload");
    }

    @Test
    void reportsDownWhenDurableProviderSyncSessionStorageIsUnreachableWithoutProviderConnectivityClaims() {
        var indicator = new ProviderSyncStorageHealthIndicator(unmigratedJdbcTemplate("provider_sync_health_down"));

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("scope", "storage-only")
                .containsEntry("storage", "provider_sync_sessions")
                .containsEntry("storageReachability", "unreachable")
                .containsEntry("errorCode", "PROVIDER_SYNC_STORAGE_UNREACHABLE")
                .doesNotContainKeys("bankConnectivity", "providerConnectivity", "networkConnectivity", "credentialStatus");
        assertThat(health.getDetails().toString())
                .doesNotContain("sandbox", "production", "credentialRef", "cursor", "rawPayload");
    }

    private static JdbcTemplate migratedJdbcTemplate(String databaseName) {
        var dataSource = dataSource(databaseName);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__create_provider_sync_sessions.sql"))
                .execute(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private static JdbcTemplate unmigratedJdbcTemplate(String databaseName) {
        return new JdbcTemplate(dataSource(databaseName));
    }

    private static DriverManagerDataSource dataSource(String databaseName) {
        return new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + "_" + java.util.UUID.randomUUID().toString().replace("-", "")
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
    }
}
