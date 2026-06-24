package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncSessionPortTest {
    private static final ProfileId PROFILE_ID = new ProfileId("pharmacy-cl");

    @TestFactory
    Stream<DynamicTest> rejectsInvalidSnapshotRequiredFields() {
        return Stream.of(
                invalidCase(
                        "blank sync id",
                        () -> snapshot("   ", PROFILE_ID, "fixture-provider", SyncSessionPort.SyncStatus.COMPLETED, 1, 1, 1, 1),
                        "Sync id is required"),
                invalidCase(
                        "missing profile id",
                        () -> snapshot("sync-001", null, "fixture-provider", SyncSessionPort.SyncStatus.COMPLETED, 1, 1, 1, 1),
                        "Profile id is required"),
                invalidCase(
                        "blank provider type",
                        () -> snapshot("sync-001", PROFILE_ID, "   ", SyncSessionPort.SyncStatus.COMPLETED, 1, 1, 1, 1),
                        "Provider type is required"),
                invalidCase(
                        "missing status",
                        () -> snapshot("sync-001", PROFILE_ID, "fixture-provider", null, 1, 1, 1, 1),
                        "Sync status is required")
        ).map(testCase -> DynamicTest.dynamicTest(testCase.name(), () -> assertThatThrownBy(testCase.snapshot())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(testCase.message())));
    }

    @TestFactory
    Stream<DynamicTest> rejectsNegativeSnapshotCounts() {
        return Stream.of(
                invalidCase(
                        "negative pages fetched",
                        () -> snapshot("sync-001", PROFILE_ID, "fixture-provider", SyncSessionPort.SyncStatus.COMPLETED, -1, 1, 1, 1),
                        "Pages fetched must not be negative"),
                invalidCase(
                        "negative entries fetched",
                        () -> snapshot("sync-001", PROFILE_ID, "fixture-provider", SyncSessionPort.SyncStatus.COMPLETED, 1, -1, 1, 1),
                        "Entries fetched must not be negative"),
                invalidCase(
                        "negative imported entries",
                        () -> snapshot("sync-001", PROFILE_ID, "fixture-provider", SyncSessionPort.SyncStatus.COMPLETED, 1, 1, -1, 1),
                        "Imported entries must not be negative"),
                invalidCase(
                        "negative session entry count",
                        () -> snapshot("sync-001", PROFILE_ID, "fixture-provider", SyncSessionPort.SyncStatus.COMPLETED, 1, 1, 1, -1),
                        "Session entry count must not be negative")
        ).map(testCase -> DynamicTest.dynamicTest(testCase.name(), () -> assertThatThrownBy(testCase.snapshot())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(testCase.message())));
    }

    private static InvalidSnapshotCase invalidCase(String name, ThrowingCallable snapshot, String message) {
        return new InvalidSnapshotCase(name, snapshot, message);
    }

    private static SyncSessionPort.SyncSessionSnapshot snapshot(
            String syncId,
            ProfileId profileId,
            String providerType,
            SyncSessionPort.SyncStatus status,
            int pagesFetched,
            int entriesFetched,
            int importedEntries,
            int sessionEntryCount
    ) {
        return new SyncSessionPort.SyncSessionSnapshot(
                syncId,
                profileId,
                providerType,
                status,
                pagesFetched,
                entriesFetched,
                importedEntries,
                false,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                sessionEntryCount,
                List.of(),
                Optional.empty(),
                SyncSessionPort.Durability.IN_MEMORY
        );
    }

    private record InvalidSnapshotCase(String name, ThrowingCallable snapshot, String message) {
    }
}
