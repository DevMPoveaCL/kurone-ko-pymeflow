package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.Instant;
import java.util.Optional;

public interface SyncSessionPort {
    String syncId(ProfileId profileId, String providerType);

    Optional<String> findCursor(ProfileId profileId, String providerType);

    void saveCursor(ProfileId profileId, String providerType, String cursor);

    Optional<Instant> lastSyncAt(ProfileId profileId, String providerType);

    void incrementEntryCount(ProfileId profileId, String providerType, int entryCount);
}
