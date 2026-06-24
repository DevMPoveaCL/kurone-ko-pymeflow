package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SyncSessionPort {
    String syncId(ProfileId profileId, String providerType);

    Optional<String> findCursor(ProfileId profileId, String providerType);

    void saveCursor(ProfileId profileId, String providerType, String cursor);

    Optional<Instant> lastSyncAt(ProfileId profileId, String providerType);

    void incrementEntryCount(ProfileId profileId, String providerType, int entryCount);

    int entryCount(ProfileId profileId, String providerType);

    void recordReport(SyncSessionSnapshot snapshot);

    Optional<SyncSessionSnapshot> findBySyncId(String syncId);

    enum SyncStatus {
        COMPLETED,
        PARTIAL,
        FAILED
    }

    enum Durability {
        IN_MEMORY,
        DURABLE
    }

    record SyncSessionSnapshot(
            String syncId,
            ProfileId profileId,
            String providerType,
            SyncStatus status,
            int pagesFetched,
            int entriesFetched,
            int importedEntries,
            boolean hasMorePages,
            boolean truncated,
            boolean authAborted,
            Optional<String> cursor,
            Optional<Instant> lastSyncAt,
            int sessionEntryCount,
            List<ProviderError> errors,
            Optional<Integer> retryAfterSeconds,
            Durability durability
    ) {
        public SyncSessionSnapshot {
            if (syncId == null || syncId.isBlank()) {
                throw new IllegalArgumentException("Sync id is required");
            }
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            if (providerType == null || providerType.isBlank()) {
                throw new IllegalArgumentException("Provider type is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("Sync status is required");
            }
            if (pagesFetched < 0) {
                throw new IllegalArgumentException("Pages fetched must not be negative");
            }
            if (entriesFetched < 0) {
                throw new IllegalArgumentException("Entries fetched must not be negative");
            }
            if (importedEntries < 0) {
                throw new IllegalArgumentException("Imported entries must not be negative");
            }
            if (sessionEntryCount < 0) {
                throw new IllegalArgumentException("Session entry count must not be negative");
            }
            providerType = providerType.trim();
            cursor = cursor == null ? Optional.empty() : cursor.map(String::trim).filter(value -> !value.isBlank());
            lastSyncAt = lastSyncAt == null ? Optional.empty() : lastSyncAt;
            errors = List.copyOf(errors == null ? List.of() : errors);
            retryAfterSeconds = retryAfterSeconds == null ? Optional.empty() : retryAfterSeconds;
            durability = durability == null ? Durability.IN_MEMORY : durability;
        }
    }
}
