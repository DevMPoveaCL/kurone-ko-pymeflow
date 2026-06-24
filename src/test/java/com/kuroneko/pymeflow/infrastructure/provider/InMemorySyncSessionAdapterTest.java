package com.kuroneko.pymeflow.infrastructure.provider;

import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemorySyncSessionAdapterTest {
    private static final ProfileId PROFILE_ID = new ProfileId("pharmacy-cl");

    private final InMemorySyncSessionAdapter adapter = new InMemorySyncSessionAdapter();

    @Test
    void savesCursorAndMarksLastSyncForResume() {
        adapter.saveCursor(PROFILE_ID, "fixture-provider", "page-2");

        assertThat(adapter.findCursor(PROFILE_ID, "fixture-provider")).contains("page-2");
        assertThat(adapter.lastSyncAt(PROFILE_ID, "fixture-provider")).isPresent();
    }

    @Test
    void trimsBlankCursorToEmptyAndIncrementsEntryCount() {
        adapter.saveCursor(PROFILE_ID, "fixture-provider", "   ");
        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 3);
        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 2);

        assertThat(adapter.findCursor(PROFILE_ID, "fixture-provider")).isEmpty();
        assertThat(adapter.entryCount(PROFILE_ID, "fixture-provider")).isEqualTo(5);
    }

    @Test
    void isolatesCursorAndEntryCountByProfileAndProviderAndCreatesSyncId() {
        adapter.saveCursor(PROFILE_ID, "fixture-provider", "fixture-cursor");
        adapter.saveCursor(PROFILE_ID, "other-provider", "other-cursor");
        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 2);
        adapter.incrementEntryCount(PROFILE_ID, "other-provider", 5);

        assertThat(adapter.findCursor(PROFILE_ID, "fixture-provider")).contains("fixture-cursor");
        assertThat(adapter.findCursor(PROFILE_ID, "other-provider")).contains("other-cursor");
        assertThat(adapter.entryCount(PROFILE_ID, "fixture-provider")).isEqualTo(2);
        assertThat(adapter.entryCount(PROFILE_ID, "other-provider")).isEqualTo(5);
        assertThat(adapter.syncId(PROFILE_ID, "fixture-provider"))
                .startsWith("sync-")
                .isNotEqualTo(adapter.syncId(PROFILE_ID, "other-provider"));
    }

    @Test
    void rejectsNegativeEntryCountIncrement() {
        assertThatThrownBy(() -> adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entry count must not be negative");
    }

    @Test
    void recordsAndFindsSnapshotBySyncIdWithCountsCursorAndRetryHint() {
        adapter.saveCursor(PROFILE_ID, "fixture-provider", "page-2");
        adapter.incrementEntryCount(PROFILE_ID, "fixture-provider", 3);
        var syncId = adapter.syncId(PROFILE_ID, "fixture-provider");
        var error = new ProviderError.RateLimitError(45, "Request limit reached");

        adapter.recordReport(new SyncSessionPort.SyncSessionSnapshot(
                syncId,
                PROFILE_ID,
                "fixture-provider",
                SyncSessionPort.SyncStatus.PARTIAL,
                2,
                3,
                3,
                true,
                true,
                false,
                Optional.of("page-2"),
                adapter.lastSyncAt(PROFILE_ID, "fixture-provider"),
                adapter.entryCount(PROFILE_ID, "fixture-provider"),
                List.of(error),
                Optional.of(45),
                SyncSessionPort.Durability.IN_MEMORY
        ));

        assertThat(adapter.findBySyncId(syncId)).hasValueSatisfying(snapshot -> {
            assertThat(snapshot.syncId()).isEqualTo(syncId);
            assertThat(snapshot.status()).isEqualTo(SyncSessionPort.SyncStatus.PARTIAL);
            assertThat(snapshot.pagesFetched()).isEqualTo(2);
            assertThat(snapshot.entriesFetched()).isEqualTo(3);
            assertThat(snapshot.importedEntries()).isEqualTo(3);
            assertThat(snapshot.hasMorePages()).isTrue();
            assertThat(snapshot.truncated()).isTrue();
            assertThat(snapshot.cursor()).contains("page-2");
            assertThat(snapshot.sessionEntryCount()).isEqualTo(3);
            assertThat(snapshot.errors()).containsExactly(error);
            assertThat(snapshot.retryAfterSeconds()).contains(45);
            assertThat(snapshot.durability()).isEqualTo(SyncSessionPort.Durability.IN_MEMORY);
        });
    }

    @Test
    void returnsEmptyForUnknownSyncIdAndNewAdapterHasNoPreviousSnapshots() {
        var syncId = adapter.syncId(PROFILE_ID, "fixture-provider");
        adapter.recordReport(new SyncSessionPort.SyncSessionSnapshot(
                syncId,
                PROFILE_ID,
                "fixture-provider",
                SyncSessionPort.SyncStatus.COMPLETED,
                1,
                1,
                1,
                false,
                false,
                false,
                Optional.empty(),
                adapter.lastSyncAt(PROFILE_ID, "fixture-provider"),
                0,
                List.of(),
                Optional.empty(),
                SyncSessionPort.Durability.IN_MEMORY
        ));

        assertThat(adapter.findBySyncId("sync-missing")).isEmpty();
        assertThat(new InMemorySyncSessionAdapter().findBySyncId(syncId)).isEmpty();
    }
}
