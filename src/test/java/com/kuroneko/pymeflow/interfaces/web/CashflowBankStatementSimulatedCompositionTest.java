package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementDraft;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementRecord;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewMovementResolutionCommand;
import com.kuroneko.pymeflow.application.cashflow.SensitiveDataPolicy;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowBankStatementSimulatedCompositionTest {

    @Test
    void reimportingSameBankTransactionIdThroughControllerAdapterAndIngestionReturnsExistingMovementWithoutDuplicateInsert() {
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profileId = new ProfileId("pharmacy-cl");
        var profile = new VerticalProfile(profileId, "Pharmacy CL", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var ingestionService = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );
        var controller = new CashflowBankStatementSimulatedController(
                new SimulatedBankStatementAdapter(ingestionService),
                new SensitiveDataPolicy(List.of())
        );
        var request = new CashflowBankStatementSimulatedController.SimulatedBankStatementRequest(
                "pharmacy-cl",
                "Cartola junio 2026",
                List.of(new CashflowBankStatementSimulatedController.SimulatedBankStatementRow(
                        "BT-COMPOSITION-001",
                        "2026-06-15",
                        "Venta POS",
                        BigDecimal.valueOf(-125000),
                        "CLP",
                        "Cuenta corriente",
                        "Farmacia Central"
                ))
        );

        var first = controller.importSimulated(request).getBody();
        var second = controller.importSimulated(request).getBody();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.categorized()).singleElement().satisfies(imported -> {
            assertThat(imported.movementId()).isEqualTo(second.categorized().getFirst().movementId());
            assertThat(imported.row()).isEqualTo(1);
            assertThat(imported.transaction().description()).isEqualTo("Farmacia Central | Venta POS");
            assertThat(imported.transaction().amount()).isEqualByComparingTo("125000");
        });
        assertThat(second.categorized()).singleElement().satisfies(reimported -> {
            assertThat(reimported.row()).isEqualTo(1);
            assertThat(reimported.transaction().description()).isEqualTo("Farmacia Central | Venta POS");
            assertThat(reimported.transaction().amount()).isEqualByComparingTo("125000");
        });
        assertThat(historyPort.insertedRecords()).hasSize(1)
                .singleElement()
                .satisfies(record -> assertThat(record.sourceReference()).isEqualTo("BT-COMPOSITION-001"));
    }

    private static final class RecordingHistoryPort implements CashflowMovementHistoryPort {
        private final List<CashflowMovementRecord> insertedRecords = new ArrayList<>();

        private List<CashflowMovementRecord> insertedRecords() {
            return insertedRecords;
        }

        @Override
        public List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts) {
            var now = Instant.now();
            var records = drafts.stream()
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
                    .toList();
            insertedRecords.addAll(records);
            return records;
        }

        @Override
        public Optional<CashflowMovementRecord> findById(UUID movementId) {
            return insertedRecords.stream()
                    .filter(record -> record.id().equals(movementId))
                    .findFirst();
        }

        @Override
        public Optional<CashflowMovementRecord> findBySourceReference(ProfileId profileId, String sourceReference) {
            return insertedRecords.stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .filter(record -> record.sourceReference().equals(sourceReference))
                    .findFirst();
        }

        @Override
        public List<CashflowMovementRecord> findByStatus(ProfileId profileId, CashflowMovementStatus status) {
            return List.of();
        }

        @Override
        public List<CashflowMovementRecord> findPendingManualReviews(ProfileId profileId) {
            return List.of();
        }

        @Override
        public List<CashflowMovementRecord> findProjectionReady(ProfileId profileId) {
            return List.of();
        }

        @Override
        public Optional<CashflowMovementRecord> resolveManualReview(ManualReviewMovementResolutionCommand command) {
            return Optional.empty();
        }
    }
}
