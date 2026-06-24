package com.kuroneko.pymeflow.infrastructure.provider;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

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
}
