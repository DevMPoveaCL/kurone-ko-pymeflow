package com.kuroneko.pymeflow.infrastructure.provider;

import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySyncSessionAdapter implements SyncSessionPort {
    private final ConcurrentMap<SyncSessionKey, SyncSessionState> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SyncSessionSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public String syncId(ProfileId profileId, String providerType) {
        var key = new SyncSessionKey(profileId, providerType);
        return sessions.computeIfAbsent(key, ignored -> SyncSessionState.empty()).syncId();
    }

    @Override
    public Optional<String> findCursor(ProfileId profileId, String providerType) {
        return Optional.ofNullable(sessions.get(new SyncSessionKey(profileId, providerType)))
                .flatMap(SyncSessionState::cursor);
    }

    @Override
    public void saveCursor(ProfileId profileId, String providerType, String cursor) {
        sessions.compute(new SyncSessionKey(profileId, providerType), (ignored, current) -> stateWithCursor(current, cursor));
    }

    @Override
    public Optional<Instant> lastSyncAt(ProfileId profileId, String providerType) {
        return Optional.ofNullable(sessions.get(new SyncSessionKey(profileId, providerType)))
                .map(SyncSessionState::lastSyncAt);
    }

    @Override
    public void incrementEntryCount(ProfileId profileId, String providerType, int entryCount) {
        if (entryCount < 0) {
            throw new IllegalArgumentException("Entry count must not be negative");
        }
        sessions.compute(new SyncSessionKey(profileId, providerType), (ignored, current) -> stateWithEntryCount(current, entryCount));
    }

    @Override
    public int entryCount(ProfileId profileId, String providerType) {
        return Optional.ofNullable(sessions.get(new SyncSessionKey(profileId, providerType)))
                .map(SyncSessionState::entryCount)
                .orElse(0);
    }

    @Override
    public void recordReport(SyncSessionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Sync session snapshot is required");
        }
        snapshots.put(snapshot.syncId(), snapshot);
    }

    @Override
    public Optional<SyncSessionSnapshot> findBySyncId(String syncId) {
        return Optional.ofNullable(syncId)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(snapshots::get);
    }

    private static SyncSessionState stateWithCursor(SyncSessionState current, String cursor) {
        var existingCount = current == null ? 0 : current.entryCount();
        var syncId = current == null ? newSyncId() : current.syncId();
        return new SyncSessionState(syncId, normalizedCursor(cursor), Instant.now(), existingCount);
    }

    private static SyncSessionState stateWithEntryCount(SyncSessionState current, int entryCount) {
        var existingCursor = current == null ? Optional.<String>empty() : current.cursor();
        var existingCount = current == null ? 0 : current.entryCount();
        var syncId = current == null ? newSyncId() : current.syncId();
        return new SyncSessionState(syncId, existingCursor, Instant.now(), existingCount + entryCount);
    }

    private static Optional<String> normalizedCursor(String cursor) {
        return Optional.ofNullable(cursor).map(String::trim).filter(value -> !value.isBlank());
    }

    private static String newSyncId() {
        return "sync-" + UUID.randomUUID();
    }

    record SyncSessionKey(ProfileId profileId, String providerType) {
        SyncSessionKey {
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            if (providerType == null || providerType.isBlank()) {
                throw new IllegalArgumentException("Provider type is required");
            }
            providerType = providerType.trim();
        }
    }

    record SyncSessionState(String syncId, Optional<String> cursor, Instant lastSyncAt, int entryCount) {
        SyncSessionState {
            if (syncId == null || syncId.isBlank()) {
                throw new IllegalArgumentException("Sync id is required");
            }
            cursor = cursor == null ? Optional.empty() : cursor.filter(value -> !value.isBlank());
            if (lastSyncAt == null) {
                throw new IllegalArgumentException("Last sync timestamp is required");
            }
            if (entryCount < 0) {
                throw new IllegalArgumentException("Entry count must not be negative");
            }
        }

        static SyncSessionState empty() {
            return new SyncSessionState(newSyncId(), Optional.empty(), Instant.now(), 0);
        }
    }
}
