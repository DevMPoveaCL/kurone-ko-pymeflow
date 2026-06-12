package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashflowMovementHistoryServiceTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final Currency CLP = Currency.getInstance("CLP");

    @Test
    void returnsOnlyPendingManualReviewMovementsWithSafeFields() {
        var service = service(new FakeHistoryPort(List.of(
                movement(CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 1", "caja-1", LocalDate.of(2026, 6, 1)),
                movement(CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 2", null, LocalDate.of(2026, 6, 2))
        )));

        var pending = service.pendingManualReviews(PROFILE_ID);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().status()).isEqualTo(CashflowMovementStatus.MANUAL_REVIEW);
        assertThat(pending.getFirst().description()).isEqualTo("Venta Caja 1");
        assertThat(pending.getFirst().sourceReference()).isEqualTo("caja-1");
    }

    @Test
    void returnsProjectionReadyTransactionsWithinDateRangeOnly() {
        var service = service(new FakeHistoryPort(List.of(
                movement(CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 1", null, LocalDate.of(2026, 6, 1)),
                movement(CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 2", null, LocalDate.of(2026, 6, 2)),
                movement(CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 3", null, LocalDate.of(2026, 6, 8)),
                movement(CashflowMovementStatus.REJECTED, null, null, null, LocalDate.of(2026, 6, 4))
        )));

        var projectionReady = service.projectionReady(
                PROFILE_ID,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3)
        );

        assertThat(projectionReady).hasSize(1);
        assertThat(projectionReady.getFirst().categoryKey()).isEqualTo("sales");
        assertThat(projectionReady.getFirst().status()).isEqualTo(CashflowMovementStatus.PROJECTABLE);
        assertThat(projectionReady.getFirst().toProjectionTransaction()).isEqualTo(new ProjectedCashflowTransaction(
                "sales",
                BigDecimal.valueOf(10_000),
                CLP,
                LocalDate.of(2026, 6, 2)
        ));
    }

    @Test
    void rejectsUnknownProfileAndInvalidDateRange() {
        var missingProfileService = new CashflowMovementHistoryService(
                new VerticalProfileService(id -> Optional.empty()),
                new FakeHistoryPort(List.of())
        );

        assertThatThrownBy(() -> missingProfileService.pendingManualReviews(PROFILE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil indicado no está configurado");

        assertThatThrownBy(() -> service(new FakeHistoryPort(List.of())).projectionReady(
                PROFILE_ID,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha final no puede ser anterior");
    }

    private static CashflowMovementHistoryService service(CashflowMovementHistoryPort port) {
        return new CashflowMovementHistoryService(new VerticalProfileService(id -> Optional.of(profile())), port);
    }

    private static CashflowMovementRecord movement(
            CashflowMovementStatus status,
            String categoryKey,
            String safeDescription,
            String sourceReference,
            LocalDate date
    ) {
        return new CashflowMovementRecord(
                UUID.randomUUID(),
                PROFILE_ID,
                BigDecimal.valueOf(10_000),
                CLP,
                date,
                status,
                categoryKey,
                safeDescription,
                sourceReference,
                status == CashflowMovementStatus.REJECTED ? "policy-blocked" : null,
                null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-01T00:00:00Z")
        );
    }

    private static VerticalProfile profile() {
        return new VerticalProfile(
                PROFILE_ID,
                "Retail",
                List.of(),
                List.of(new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW)),
                List.of()
        );
    }

    private record FakeHistoryPort(List<CashflowMovementRecord> records) implements CashflowMovementHistoryPort {
        @Override
        public List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public Optional<CashflowMovementRecord> findById(UUID movementId) {
            return Optional.empty();
        }

        @Override
        public List<CashflowMovementRecord> findPendingManualReviews(ProfileId profileId) {
            return records.stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .filter(record -> record.status() == CashflowMovementStatus.MANUAL_REVIEW)
                    .toList();
        }

        @Override
        public List<CashflowMovementRecord> findProjectionReady(ProfileId profileId) {
            return records.stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .toList();
        }

        @Override
        public Optional<CashflowMovementRecord> resolveManualReview(ManualReviewMovementResolutionCommand command) {
            return Optional.empty();
        }
    }
}
