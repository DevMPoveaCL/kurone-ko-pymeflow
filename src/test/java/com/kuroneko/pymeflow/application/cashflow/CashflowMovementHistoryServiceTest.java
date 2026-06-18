package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
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

    @Test
    void resolvesPendingManualReviewByIdIntoProjectionReadyTransaction() {
        var movement = movement(CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 1", "caja-1", LocalDate.of(2026, 6, 1));
        var port = new FakeHistoryPort(List.of(movement));
        var service = service(port);

        var result = service.resolveManualReview(new ManualReviewMovementResolutionCommand(
                movement.id(),
                PROFILE_ID,
                "sales"
        ));

        assertThat(result.transaction().movementId()).isEqualTo(movement.id());
        assertThat(result.transaction().status()).isEqualTo(CashflowMovementStatus.PROJECTABLE);
        assertThat(result.transaction().categoryKey()).isEqualTo("sales");
        assertThat(result.category().key()).isEqualTo("sales");
        assertThat(result.safeDescription()).contains("Venta Caja 1");
        assertThat(result.safeSourceReference()).contains("caja-1");
        assertThat(port.findProjectionReady(PROFILE_ID)).extracting(CashflowMovementRecord::id).contains(movement.id());
    }

    @Test
    void returnsFallbackGeneratedSourceReferenceWhenHistoryIsQueried() {
        var profileService = new VerticalProfileService(id -> Optional.of(profile()));
        var port = new FakeHistoryPort(List.of());
        var ingestionService = new CashflowIngestionService(
                profileService,
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.empty(), true),
                new SensitiveDataPolicy(List.of()),
                port
        );
        var historyService = new CashflowMovementHistoryService(profileService, port);
        var transaction = new Transaction(
                "Pago farmacia",
                BigDecimal.valueOf(1000),
                CLP,
                LocalDate.of(2024, 6, 18)
        );
        var expectedReference = TransactionFingerprint.compute(PROFILE_ID, transaction);

        ingestionService.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                PROFILE_ID,
                List.of(new CashflowIngestionService.CashflowIngestionCommand.IngestionItem(transaction, null))
        ));

        assertThat(historyService.pendingManualReviews(PROFILE_ID)).singleElement().satisfies(movement -> {
            assertThat(movement.description()).isEqualTo("Pago farmacia");
            assertThat(movement.sourceReference()).isEqualTo(expectedReference);
            assertThat(movement.sourceReference()).startsWith("fp:v1:");
        });
    }

    @Test
    void rejectsUnknownMovementDoubleResolutionRejectedAndInvalidCategory() {
        var pending = movement(CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 1", null, LocalDate.of(2026, 6, 1));
        var projectable = movement(CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 2", null, LocalDate.of(2026, 6, 2));
        var rejected = movement(CashflowMovementStatus.REJECTED, null, null, null, LocalDate.of(2026, 6, 3));
        var service = service(new FakeHistoryPort(List.of(pending, projectable, rejected)));

        assertThatThrownBy(() -> service.resolveManualReview(new ManualReviewMovementResolutionCommand(
                UUID.randomUUID(), PROFILE_ID, "sales")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró el movimiento solicitado");
        assertThatThrownBy(() -> service.resolveManualReview(new ManualReviewMovementResolutionCommand(
                projectable.id(), PROFILE_ID, "sales")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movimiento ya fue resuelto");
        assertThatThrownBy(() -> service.resolveManualReview(new ManualReviewMovementResolutionCommand(
                rejected.id(), PROFILE_ID, "sales")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movimiento ya fue resuelto");
        assertThatThrownBy(() -> service.resolveManualReview(new ManualReviewMovementResolutionCommand(
                pending.id(), PROFILE_ID, "unknown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoría enviada no está configurada");
    }

    @Test
    void rejectsSensitiveResolutionInputBeforeUpdatingMovement() {
        var movement = movement(CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 1", null, LocalDate.of(2026, 6, 1));
        var port = new FakeHistoryPort(List.of(movement));
        var service = new CashflowMovementHistoryService(
                new VerticalProfileService(id -> Optional.of(profile())),
                port,
                new SensitiveDataPolicy(List.of("receta"))
        );

        assertThatThrownBy(() -> service.resolveManualReview(new ManualReviewMovementResolutionCommand(
                movement.id(), PROFILE_ID, "sales", "Venta con receta 123", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datos sensibles");
        assertThat(port.findById(movement.id()).orElseThrow().status()).isEqualTo(CashflowMovementStatus.MANUAL_REVIEW);
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

    private static final class FakeHistoryPort implements CashflowMovementHistoryPort {
        private final java.util.Map<UUID, CashflowMovementRecord> records;

        private FakeHistoryPort(List<CashflowMovementRecord> records) {
            this.records = records.stream().collect(java.util.stream.Collectors.toMap(CashflowMovementRecord::id, record -> record));
        }

        @Override
        public List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts) {
            var now = Instant.parse("2026-06-01T00:00:00Z");
            return drafts.stream()
                    .map(draft -> new CashflowMovementRecord(
                            UUID.randomUUID(),
                            draft.profileId(),
                            draft.amount(),
                            draft.currency(),
                            draft.date(),
                            draft.status(),
                            draft.categoryKey(),
                            draft.safeDescription(),
                            draft.sourceReference(),
                            draft.rejectionReasonCode(),
                            null,
                            now,
                            now
                    ))
                    .peek(record -> records.put(record.id(), record))
                    .toList();
        }

        @Override
        public Optional<CashflowMovementRecord> findById(UUID movementId) {
            return Optional.ofNullable(records.get(movementId));
        }

        @Override
        public Optional<CashflowMovementRecord> findBySourceReference(ProfileId profileId, String sourceReference) {
            return records.values().stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .filter(record -> sourceReference.equals(record.sourceReference()))
                    .findFirst();
        }

        @Override
        public List<CashflowMovementRecord> findPendingManualReviews(ProfileId profileId) {
            return records.values().stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .filter(record -> record.status() == CashflowMovementStatus.MANUAL_REVIEW)
                    .toList();
        }

        @Override
        public List<CashflowMovementRecord> findProjectionReady(ProfileId profileId) {
            return records.values().stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .filter(record -> record.status() == CashflowMovementStatus.PROJECTABLE)
                    .toList();
        }

        @Override
        public Optional<CashflowMovementRecord> resolveManualReview(ManualReviewMovementResolutionCommand command) {
            var current = records.get(command.movementId());
            if (current == null || !current.profileId().equals(command.profileId()) || current.status() != CashflowMovementStatus.MANUAL_REVIEW) {
                return Optional.empty();
            }
            var resolved = new CashflowMovementRecord(
                    current.id(),
                    current.profileId(),
                    current.amount(),
                    current.currency(),
                    current.date(),
                    CashflowMovementStatus.PROJECTABLE,
                    command.categoryKey(),
                    current.safeDescription(),
                    current.sourceReference(),
                    current.rejectionReasonCode(),
                    Instant.parse("2026-06-02T00:00:00Z"),
                    current.createdAt(),
                    Instant.parse("2026-06-02T00:00:00Z")
            );
            records.put(command.movementId(), resolved);
            return Optional.of(resolved);
        }
    }
}
