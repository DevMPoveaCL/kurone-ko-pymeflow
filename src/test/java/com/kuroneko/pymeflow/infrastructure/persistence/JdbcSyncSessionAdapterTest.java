package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcSyncSessionAdapterTest {
    private static final ProfileId PROFILE_ID = new ProfileId("pharmacy-cl");

    private JdbcTemplate jdbcTemplate;
    private JdbcSyncSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        var databaseName = "provider_sync_session_adapter_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__create_provider_sync_sessions.sql"))
                .execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        adapter = new JdbcSyncSessionAdapter(jdbcTemplate);
    }

    @Test
    void keepsStableSyncIdAndResumesCursorAfterNewAdapterInstance() {
        var syncId = adapter.syncId(PROFILE_ID, "fixture-provider");
        adapter.saveCursor(PROFILE_ID, "fixture-provider", "page-2");

        var restartedAdapter = new JdbcSyncSessionAdapter(jdbcTemplate);

        assertThat(restartedAdapter.syncId(PROFILE_ID, "fixture-provider")).isEqualTo(syncId);
        assertThat(restartedAdapter.findCursor(PROFILE_ID, "fixture-provider")).contains("page-2");
        assertThat(restartedAdapter.lastSyncAt(PROFILE_ID, "fixture-provider")).isPresent();
    }

    @Test
    void storesBlankCursorAsNullAndReturnsDurableSnapshotBySyncId() {
        var syncId = adapter.syncId(PROFILE_ID, "fixture-provider");
        adapter.saveCursor(PROFILE_ID, "fixture-provider", "   ");
        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 4);
        var error = new ProviderError.RateLimitError(60, "Request limit reached");

        adapter.recordReport(new SyncSessionPort.SyncSessionSnapshot(
                syncId,
                PROFILE_ID,
                "fixture-provider",
                SyncSessionPort.SyncStatus.PARTIAL,
                2,
                4,
                3,
                true,
                true,
                false,
                Optional.empty(),
                adapter.lastSyncAt(PROFILE_ID, "fixture-provider"),
                adapter.entryCount(PROFILE_ID, "fixture-provider"),
                List.of(error),
                Optional.of(60),
                SyncSessionPort.Durability.DURABLE
        ));

        assertThat(adapter.findCursor(PROFILE_ID, "fixture-provider")).isEmpty();
        assertThat(adapter.findBySyncId(syncId)).hasValueSatisfying(snapshot -> {
            assertThat(snapshot.status()).isEqualTo(SyncSessionPort.SyncStatus.PARTIAL);
            assertThat(snapshot.pagesFetched()).isEqualTo(2);
            assertThat(snapshot.entriesFetched()).isEqualTo(4);
            assertThat(snapshot.importedEntries()).isEqualTo(3);
            assertThat(snapshot.hasMorePages()).isTrue();
            assertThat(snapshot.truncated()).isTrue();
            assertThat(snapshot.authAborted()).isFalse();
            assertThat(snapshot.cursor()).isEmpty();
            assertThat(snapshot.sessionEntryCount()).isEqualTo(4);
            assertThat(snapshot.errors()).containsExactly(error);
            assertThat(snapshot.retryAfterSeconds()).contains(60);
            assertThat(snapshot.durability()).isEqualTo(SyncSessionPort.Durability.DURABLE);
        });
    }

    @Test
    void rejectsNegativeCountAndAccumulatesEntryCountAtomically() {
        assertThatThrownBy(() -> adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entry count must not be negative");

        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 2);
        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 5);

        assertThat(adapter.entryCount(PROFILE_ID, "fixture-provider")).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                select session_entry_count
                from provider_sync_sessions
                where profile_id = ? and provider_type = ?
                """, Integer.class, PROFILE_ID.value(), "fixture-provider"))
                .isEqualTo(7);
    }

    @Test
    void failsSafelyWhenDurableSessionStorageIsUnavailable() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:provider_sync_session_missing_storage_" + java.util.UUID.randomUUID().toString().replace("-", "")
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        var unmigratedAdapter = new JdbcSyncSessionAdapter(new JdbcTemplate(dataSource));

        assertThatThrownBy(() -> unmigratedAdapter.syncId(PROFILE_ID, "fixture-provider"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("provider_sync_sessions");
        assertThatThrownBy(() -> unmigratedAdapter.findBySyncId("sync-missing-storage"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("provider_sync_sessions");
    }
}
