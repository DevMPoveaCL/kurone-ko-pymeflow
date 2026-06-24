package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;

import java.util.Optional;

public final class ProviderSyncStatusUseCase {
    private final SyncSessionPort syncSessionPort;

    public ProviderSyncStatusUseCase(SyncSessionPort syncSessionPort) {
        if (syncSessionPort == null) {
            throw new IllegalArgumentException("Sync session port is required");
        }
        this.syncSessionPort = syncSessionPort;
    }

    public Optional<SyncSessionPort.SyncSessionSnapshot> find(String syncId) {
        if (syncId == null || syncId.isBlank()) {
            throw new IllegalArgumentException("Sync id is required");
        }
        return syncSessionPort.findBySyncId(syncId.trim());
    }
}
