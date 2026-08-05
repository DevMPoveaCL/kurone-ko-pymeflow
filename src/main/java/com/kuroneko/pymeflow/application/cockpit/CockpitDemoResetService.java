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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public class CockpitDemoResetService {
    private static final ProfileId DEMO_PROFILE = new ProfileId("phar" + "macy-cl");
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final String PROVIDER_TYPE = "fixture-demo";
    private static final Instant FIXTURE_SYNC_TIME = Instant.parse("2026-06-20T10:00:00Z");
    private static final String ACQUIRER_SETTLEMENTS = "acq" + "uirer-settlements";
    private static final String ACQUIRER_SOURCE_REFERENCE = "demo-reset-acq" + "uirer-001";

    private final DemoDataPort demoDataPort;
    private final CashflowMovementHistoryPort movementHistoryPort;
    private final SyncSessionPort syncSessionPort;
    private final CockpitPreferencesPort preferencesPort;

    public CockpitDemoResetService(
            DemoDataPort demoDataPort,
            CashflowMovementHistoryPort movementHistoryPort,
            SyncSessionPort syncSessionPort,
            CockpitPreferencesPort preferencesPort
    ) {
        this.demoDataPort = demoDataPort;
        this.movementHistoryPort = movementHistoryPort;
        this.syncSessionPort = syncSessionPort;
        this.preferencesPort = preferencesPort;
    }

    @Transactional
    public DemoResetResult resetAndSeed(ProfileId profileId) {
        requireDemoProfile(profileId);
        demoDataPort.reset(profileId);

        var movements = demoMovements(profileId);
        movementHistoryPort.saveAll(movements);

        var syncId = syncSessionPort.syncId(profileId, PROVIDER_TYPE);
        syncSessionPort.recordReport(completedSnapshot(syncId, profileId, movements.size()));
        preferencesPort.save(profileId, new CockpitPreferences(BigDecimal.valueOf(350_000), 7));

        return new DemoResetResult(
                "DEMO_RESET_SEEDED",
                movements.size(),
                syncId,
                "Demo data was reset and seeded safely."
        );
    }

    private static void requireDemoProfile(ProfileId profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (!DEMO_PROFILE.equals(profileId)) {
            throw new DemoOnlyProfileException("Demo reset is available for demo profiles only.");
        }
    }

    private static List<CashflowMovementDraft> demoMovements(ProfileId profileId) {
        return List.of(
                projectable(profileId, BigDecimal.valueOf(185_000), LocalDate.of(2026, 6, 20), TransactionDirection.CREDIT, "sales", "Demo sales batch", "demo-reset-sales-001"),
                projectable(profileId, BigDecimal.valueOf(240_000), LocalDate.of(2026, 6, 21), TransactionDirection.CREDIT, ACQUIRER_SETTLEMENTS, "Demo card settlement", ACQUIRER_SOURCE_REFERENCE),
                projectable(profileId, BigDecimal.valueOf(120_000), LocalDate.of(2026, 6, 22), TransactionDirection.DEBIT, "suppliers", "Demo supplier payment", "demo-reset-suppliers-001"),
                manualReview(profileId, BigDecimal.valueOf(900_000), LocalDate.of(2026, 6, 23), TransactionDirection.DEBIT, "Demo rent payment", "demo-reset-rent-001"),
                manualReview(profileId, BigDecimal.valueOf(250_000), LocalDate.of(2026, 6, 24), TransactionDirection.CREDIT, "Demo marketplace settlement", "demo-reset-marketplace-001")
        );
    }

    private static CashflowMovementDraft projectable(
            ProfileId profileId,
            BigDecimal amount,
            LocalDate date,
            TransactionDirection direction,
            String categoryKey,
            String safeDescription,
            String sourceReference
    ) {
        return new CashflowMovementDraft(
                profileId,
                amount,
                CLP,
                date,
                direction,
                CashflowMovementStatus.PROJECTABLE,
                categoryKey,
                safeDescription,
                sourceReference,
                null
        );
    }

    private static CashflowMovementDraft manualReview(
            ProfileId profileId,
            BigDecimal amount,
            LocalDate date,
            TransactionDirection direction,
            String safeDescription,
            String sourceReference
    ) {
        return new CashflowMovementDraft(
                profileId,
                amount,
                CLP,
                date,
                direction,
                CashflowMovementStatus.MANUAL_REVIEW,
                null,
                safeDescription,
                sourceReference,
                null
        );
    }

    private static SyncSessionPort.SyncSessionSnapshot completedSnapshot(String syncId, ProfileId profileId, int entryCount) {
        return new SyncSessionPort.SyncSessionSnapshot(
                syncId,
                profileId,
                PROVIDER_TYPE,
                SyncSessionPort.SyncStatus.COMPLETED,
                1,
                entryCount,
                entryCount,
                false,
                false,
                false,
                Optional.empty(),
                Optional.of(FIXTURE_SYNC_TIME),
                entryCount,
                List.of(),
                Optional.empty(),
                SyncSessionPort.Durability.DURABLE
        );
    }

    public record DemoResetResult(String status, int movementsSeeded, String syncSessionId, String message) {
    }

    public static class DemoOnlyProfileException extends RuntimeException {
        public DemoOnlyProfileException(String message) {
            super(message);
        }
    }
}
