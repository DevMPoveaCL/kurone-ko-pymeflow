package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderSyncStatusUseCaseTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");

    private final SyncSessionPort syncSessionPort = mock(SyncSessionPort.class);
    private final ProviderSyncStatusUseCase useCase = new ProviderSyncStatusUseCase(syncSessionPort);

    @Test
    void findsSafeSnapshotBySyncId() {
        var snapshot = snapshot("sync-found-001", SyncSessionPort.SyncStatus.COMPLETED, List.of(), Optional.empty());
        when(syncSessionPort.findBySyncId("sync-found-001")).thenReturn(Optional.of(snapshot));

        var result = useCase.find("sync-found-001");

        assertThat(result).contains(snapshot);
        verify(syncSessionPort).findBySyncId("sync-found-001");
    }

    @Test
    void returnsEmptyForUnknownOrExpiredSyncId() {
        when(syncSessionPort.findBySyncId("sync-missing-001")).thenReturn(Optional.empty());

        var result = useCase.find("sync-missing-001");

        assertThat(result).isEmpty();
        verify(syncSessionPort).findBySyncId("sync-missing-001");
    }

    @Test
    void exposesProviderErrorAndRetryMetadataWithoutCredentialFields() {
        var error = new ProviderError.RateLimitError(60, "Request limit reached");
        var snapshot = snapshot("sync-rate-limited-001", SyncSessionPort.SyncStatus.FAILED, List.of(error), Optional.of(60));
        when(syncSessionPort.findBySyncId("sync-rate-limited-001")).thenReturn(Optional.of(snapshot));

        var result = useCase.find("sync-rate-limited-001");

        assertThat(result).hasValueSatisfying(found -> {
            assertThat(found.status()).isEqualTo(SyncSessionPort.SyncStatus.FAILED);
            assertThat(found.errors()).containsExactly(error);
            assertThat(found.retryAfterSeconds()).contains(60);
            assertThat(found.toString()).doesNotContain("credential-ref", "secret", "token");
        });
    }

    @Test
    void rejectsBlankSyncIdBeforeCallingPort() {
        assertThatThrownBy(() -> useCase.find("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sync id is required");
    }

    private static SyncSessionPort.SyncSessionSnapshot snapshot(
            String syncId,
            SyncSessionPort.SyncStatus status,
            List<ProviderError> errors,
            Optional<Integer> retryAfterSeconds
    ) {
        return new SyncSessionPort.SyncSessionSnapshot(
                syncId,
                PROFILE_ID,
                "fixture-provider",
                status,
                1,
                2,
                2,
                false,
                false,
                false,
                Optional.empty(),
                Optional.of(Instant.parse("2026-06-23T10:15:30Z")),
                2,
                errors,
                retryAfterSeconds,
                SyncSessionPort.Durability.IN_MEMORY
        );
    }
}
