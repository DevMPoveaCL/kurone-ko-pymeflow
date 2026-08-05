package com.kuroneko.pymeflow.application.cockpit;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementDraft;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.port.out.CockpitPreferencesPort;
import com.kuroneko.pymeflow.application.port.out.DemoDataPort;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CockpitDemoResetServiceTest {
    private static final ProfileId DEMO_PROFILE = new ProfileId("pharmacy-cl");

    private final DemoDataPort demoDataPort = mock(DemoDataPort.class);
    private final CashflowMovementHistoryPort movementHistoryPort = mock(CashflowMovementHistoryPort.class);
    private final SyncSessionPort syncSessionPort = mock(SyncSessionPort.class);
    private final CockpitPreferencesPort preferencesPort = mock(CockpitPreferencesPort.class);
    private final CockpitDemoResetService service = new CockpitDemoResetService(
            demoDataPort,
            movementHistoryPort,
            syncSessionPort,
            preferencesPort
    );

    @Test
    void resetsBeforeSeedingAndReturnsSafeCounts() {
        when(syncSessionPort.syncId(DEMO_PROFILE, "fixture-demo")).thenReturn("sync-demo-001");

        var result = service.resetAndSeed(DEMO_PROFILE);

        var movementsCaptor = movementsCaptor();
        var syncCaptor = syncCaptor();
        var preferencesProfileCaptor = preferencesProfileCaptor();
        var preferencesCaptor = preferencesCaptor();
        InOrder inOrder = inOrder(demoDataPort, movementHistoryPort, syncSessionPort, preferencesPort);
        inOrder.verify(demoDataPort).reset(DEMO_PROFILE);
        inOrder.verify(movementHistoryPort).saveAll(movementsCaptor.capture());
        inOrder.verify(syncSessionPort).syncId(DEMO_PROFILE, "fixture-demo");
        inOrder.verify(syncSessionPort).recordReport(syncCaptor.capture());
        inOrder.verify(preferencesPort).save(preferencesProfileCaptor.capture(), preferencesCaptor.capture());

        assertThat(result.status()).isEqualTo("DEMO_RESET_SEEDED");
        assertThat(result.movementsSeeded()).isEqualTo(5);
        assertThat(result.syncSessionId()).isEqualTo("sync-demo-001");
        assertThat(result.message()).isEqualTo("Demo data was reset and seeded safely.");
        assertThat(result.toString()).doesNotContain("password", "token", "credential");
    }

    @Test
    void seedsDeterministicProjectableAndManualReviewMovements() {
        when(syncSessionPort.syncId(DEMO_PROFILE, "fixture-demo")).thenReturn("sync-demo-001");

        service.resetAndSeed(DEMO_PROFILE);

        var movementsCaptor = movementsCaptor();
        verify(movementHistoryPort).saveAll(movementsCaptor.capture());
        var movements = movementsCaptor.getValue();
        assertThat(movements).hasSize(5);
        assertThat(movements).extracting(CashflowMovementDraft::profileId).containsOnly(DEMO_PROFILE);
        assertThat(movements).extracting(CashflowMovementDraft::status)
                .containsExactly(
                        CashflowMovementStatus.PROJECTABLE,
                        CashflowMovementStatus.PROJECTABLE,
                        CashflowMovementStatus.PROJECTABLE,
                        CashflowMovementStatus.MANUAL_REVIEW,
                        CashflowMovementStatus.MANUAL_REVIEW
                );
        assertThat(movements).extracting(CashflowMovementDraft::categoryKey)
                .containsExactly("sales", "acquirer-settlements", "suppliers", null, null);
        assertThat(movements).extracting(CashflowMovementDraft::amount)
                .containsExactly(
                        BigDecimal.valueOf(185_000),
                        BigDecimal.valueOf(240_000),
                        BigDecimal.valueOf(120_000),
                        BigDecimal.valueOf(900_000),
                        BigDecimal.valueOf(250_000)
                );
        assertThat(movements).extracting(CashflowMovementDraft::safeDescription)
                .containsExactly(
                        "Demo sales batch",
                        "Demo card settlement",
                        "Demo supplier payment",
                        "Demo rent payment",
                        "Demo marketplace settlement"
                );
        assertThat(movements).extracting(CashflowMovementDraft::direction)
                .containsExactly(
                        TransactionDirection.CREDIT,
                        TransactionDirection.CREDIT,
                        TransactionDirection.DEBIT,
                        TransactionDirection.DEBIT,
                        TransactionDirection.CREDIT
                );
        assertThat(movements).extracting(CashflowMovementDraft::sourceReference)
                .containsExactly(
                        "demo-reset-sales-001",
                        "demo-reset-acquirer-001",
                        "demo-reset-suppliers-001",
                        "demo-reset-rent-001",
                        "demo-reset-marketplace-001"
                );
        assertThat(movements).filteredOn(movement -> movement.status() == CashflowMovementStatus.PROJECTABLE)
                .hasSize(3);
        assertThat(movements).filteredOn(movement -> movement.status() == CashflowMovementStatus.MANUAL_REVIEW)
                .hasSize(2);
        assertThat(movements)
                .filteredOn(movement -> movement.status() == CashflowMovementStatus.MANUAL_REVIEW)
                .filteredOn(movement -> movement.direction() == TransactionDirection.CREDIT)
                .hasSize(1);
        assertThat(movements)
                .filteredOn(movement -> movement.status() == CashflowMovementStatus.MANUAL_REVIEW)
                .filteredOn(movement -> movement.direction() == TransactionDirection.DEBIT)
                .hasSize(1);
        assertThat(movements).filteredOn(movement -> movement.direction() == TransactionDirection.CREDIT)
                .hasSize(3);
        assertThat(movements).filteredOn(movement -> movement.direction() == TransactionDirection.DEBIT)
                .hasSize(2);

        assertThat(movements.stream()
                .filter(movement -> movement.status() == CashflowMovementStatus.PROJECTABLE)
                .filter(movement -> movement.direction() == TransactionDirection.CREDIT)
                .map(CashflowMovementDraft::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("425000");
        assertThat(movements.stream()
                .filter(movement -> movement.status() == CashflowMovementStatus.PROJECTABLE)
                .filter(movement -> movement.direction() == TransactionDirection.DEBIT)
                .map(CashflowMovementDraft::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("120000");
        assertThat(movements.stream()
                .filter(movement -> movement.direction() == TransactionDirection.CREDIT)
                .map(CashflowMovementDraft::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("675000");
        assertThat(movements.stream()
                .filter(movement -> movement.direction() == TransactionDirection.DEBIT)
                .map(CashflowMovementDraft::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1020000");
    }

    @Test
    void seedsCompletedSyncSnapshotAndDefaultPreferences() {
        when(syncSessionPort.syncId(DEMO_PROFILE, "fixture-demo")).thenReturn("sync-demo-001");

        service.resetAndSeed(DEMO_PROFILE);

        var syncCaptor = syncCaptor();
        var preferencesProfileCaptor = preferencesProfileCaptor();
        var preferencesCaptor = preferencesCaptor();
        verify(syncSessionPort).recordReport(syncCaptor.capture());
        verify(preferencesPort).save(preferencesProfileCaptor.capture(), preferencesCaptor.capture());
        var snapshot = syncCaptor.getValue();
        assertThat(snapshot.syncId()).isEqualTo("sync-demo-001");
        assertThat(snapshot.profileId()).isEqualTo(DEMO_PROFILE);
        assertThat(snapshot.providerType()).isEqualTo("fixture-demo");
        assertThat(snapshot.status()).isEqualTo(SyncSessionPort.SyncStatus.COMPLETED);
        assertThat(snapshot.pagesFetched()).isEqualTo(1);
        assertThat(snapshot.entriesFetched()).isEqualTo(5);
        assertThat(snapshot.importedEntries()).isEqualTo(5);
        assertThat(snapshot.errors()).isEmpty();

        assertThat(preferencesProfileCaptor.getValue()).isEqualTo(DEMO_PROFILE);
        assertThat(preferencesCaptor.getValue())
                .isEqualTo(new CockpitPreferences(BigDecimal.valueOf(350_000), 7));
    }

    @Test
    void rejectsNonDemoProfilesBeforeDeletingData() {
        var nonDemoProfile = new ProfileId("retail-cl");

        assertThatThrownBy(() -> service.resetAndSeed(nonDemoProfile))
                .isInstanceOf(CockpitDemoResetService.DemoOnlyProfileException.class)
                .hasMessageContaining("demo profiles only");
    }

    private ArgumentCaptor<List<CashflowMovementDraft>> movementsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private ArgumentCaptor<SyncSessionPort.SyncSessionSnapshot> syncCaptor() {
        return ArgumentCaptor.forClass(SyncSessionPort.SyncSessionSnapshot.class);
    }

    private ArgumentCaptor<ProfileId> preferencesProfileCaptor() {
        return ArgumentCaptor.forClass(ProfileId.class);
    }

    private ArgumentCaptor<CockpitPreferences> preferencesCaptor() {
        return ArgumentCaptor.forClass(CockpitPreferences.class);
    }
}
