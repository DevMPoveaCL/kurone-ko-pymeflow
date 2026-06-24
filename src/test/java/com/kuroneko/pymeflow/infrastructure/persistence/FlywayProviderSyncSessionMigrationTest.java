package com.kuroneko.pymeflow.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayProviderSyncSessionMigrationTest {

    @Test
    void createsProviderSyncSessionStorageWithRequiredColumnsAndIndexes() {
        var jdbcTemplate = migratedJdbcTemplate();

        assertThat(columnNames(jdbcTemplate)).contains(
                "sync_id",
                "profile_id",
                "provider_type",
                "status",
                "pages_fetched",
                "entries_fetched",
                "imported_entries",
                "has_more_pages",
                "truncated",
                "auth_aborted",
                "cursor",
                "last_sync_at",
                "session_entry_count",
                "retry_after_seconds",
                "errors_json",
                "created_at",
                "updated_at"
        );
        assertThat(indexNames(jdbcTemplate)).contains(
                "provider_sync_sessions_profile_provider_uk",
                "provider_sync_sessions_profile_provider_idx"
        );
    }

    @Test
    void enforcesStatusCheckAndUniqueProfileProviderCursorState() {
        var jdbcTemplate = migratedJdbcTemplate();

        jdbcTemplate.update("""
                        insert into provider_sync_sessions
                        (sync_id, profile_id, provider_type, status, created_at, updated_at)
                        values ('sync-001', 'pharmacy-cl', 'fixture-provider', 'COMPLETED', current_timestamp, current_timestamp)
                        """);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from provider_sync_sessions where sync_id = 'sync-001'",
                Integer.class
        )).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update("""
                        insert into provider_sync_sessions
                        (sync_id, profile_id, provider_type, status, created_at, updated_at)
                        values ('sync-002', 'pharmacy-cl', 'fixture-provider', 'COMPLETED', current_timestamp, current_timestamp)
                        """))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update("""
                        insert into provider_sync_sessions
                        (sync_id, profile_id, provider_type, status, created_at, updated_at)
                        values ('sync-003', 'pharmacy-cl', 'other-provider', 'RUNNING', current_timestamp, current_timestamp)
                        """))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    private static JdbcTemplate migratedJdbcTemplate() {
        var databaseName = "provider_sync_session_migration_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__create_provider_sync_sessions.sql"))
                .execute(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private static java.util.List<String> columnNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_name = 'provider_sync_sessions'
                order by ordinal_position
                """, String.class);
    }

    private static java.util.List<String> indexNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("""
                select index_name
                from information_schema.indexes
                where table_name = 'provider_sync_sessions'
                """, String.class);
    }
}
