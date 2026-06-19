package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
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
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowIngestionServiceTest {

    @Test
    void categorizesAcceptedTransactionsAndRejectsSensitiveIdentifiers() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var profileService = new VerticalProfileService(id -> Optional.of(profile));
        CashflowCategorizationPort categorizationPort = (transaction, loadedProfile) ->
                new CategoryAssignment(Optional.of(category), false);
        var service = new CashflowIngestionService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of("blocked-token")),
                new RecordingHistoryPort()
        );
        var accepted = transaction("Venta Caja 1");
        var rejected = transaction("Venta Caja 1 blocked-token");

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(accepted), item(rejected))
        ));

        assertThat(result.categorized()).singleElement()
                .extracting(CashflowIngestionService.CategorizedTransaction::transaction)
                .isEqualTo(accepted);
        assertThat(result.manualReview()).isEmpty();
        assertThat(result.rejected()).singleElement()
                .extracting(CashflowIngestionService.RejectedTransaction::transaction)
                .isEqualTo(rejected);
    }

    @Test
    void partitionsUnmatchedTransactionsIntoManualReview() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var profileService = new VerticalProfileService(id -> Optional.of(profile));
        CashflowCategorizationPort categorizationPort = (transaction, loadedProfile) ->
                new CategoryAssignment(Optional.empty(), true);
        var service = new CashflowIngestionService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of()),
                new RecordingHistoryPort()
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Unmatched movement")))
        ));

        assertThat(result.categorized()).isEmpty();
        assertThat(result.manualReview()).singleElement()
                .extracting(CashflowIngestionService.ManualReviewTransaction::assignment)
                .satisfies(assignment -> {
                    assertThat(assignment.category()).isEmpty();
                    assertThat(assignment.requiresManualReview()).isTrue();
                });
    }

    @Test
    void sensitiveTransactionsBypassCategorization() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var profileService = new VerticalProfileService(id -> Optional.of(profile));
        var categorizationCalls = new AtomicInteger();
        CashflowCategorizationPort categorizationPort = (transaction, loadedProfile) -> {
            categorizationCalls.incrementAndGet();
            return new CategoryAssignment(Optional.of(category), false);
        };
        var service = new CashflowIngestionService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of("blocked-token")),
                new RecordingHistoryPort()
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Venta Caja 1 blocked-token")))
        ));

        assertThat(categorizationCalls).hasValue(0);
        assertThat(result.categorized()).isEmpty();
        assertThat(result.manualReview()).isEmpty();
        assertThat(result.rejected()).hasSize(1);
    }

    @Test
    void persistsCategorizedManualReviewAndRejectedOutcomesWithSafeFields() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var profileService = new VerticalProfileService(id -> Optional.of(profile));
        CashflowCategorizationPort categorizationPort = (transaction, loadedProfile) -> {
            if (transaction.description().contains("Manual")) {
                return new CategoryAssignment(Optional.empty(), true);
            }
            return new CategoryAssignment(Optional.of(category), false);
        };
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of("blocked-token")),
                historyPort
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(
                        item(transaction("Venta Caja 1")),
                        item(transaction("Manual Caja 1")),
                        item(transaction("Venta Caja 1 blocked-token"))
                )
        ));

        assertThat(historyPort.drafts)
                .extracting(CashflowMovementDraft::status)
                .containsExactly(
                        CashflowMovementStatus.PROJECTABLE,
                        CashflowMovementStatus.MANUAL_REVIEW,
                        CashflowMovementStatus.REJECTED
                );
        assertThat(historyPort.drafts.get(0).categoryKey()).isEqualTo("sales");
        assertThat(historyPort.drafts.get(0).safeDescription()).isEqualTo("Venta Caja 1");
        assertThat(historyPort.drafts.get(1).safeDescription()).isEqualTo("Manual Caja 1");
        assertThat(historyPort.drafts.get(2).safeDescription()).isNull();
        assertThat(historyPort.drafts.get(2).rejectionReasonCode()).isEqualTo("SENSITIVE_IDENTIFIER_REJECTED");
        assertThat(result.categorized()).singleElement().satisfies(item -> assertThat(item.movementId()).isNotNull());
        assertThat(result.manualReview()).singleElement().satisfies(item -> assertThat(item.movementId()).isNotNull());
        assertThat(result.rejected()).singleElement().satisfies(item -> assertThat(item.movementId()).isNotNull());
    }

    @Test
    void duplicateExternalReferenceReturnsExistingMovementWithoutInsert() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var existing = record(profileId, CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 1", "batch-001", null);
        historyPort.existing.add(existing);
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Venta Caja 1 modificada"), "batch-001"))
        ));

        assertThat(historyPort.drafts).isEmpty();
        assertThat(result.categorized()).singleElement().satisfies(duplicate -> {
            assertThat(duplicate.movementId()).isEqualTo(existing.id());
            assertThat(duplicate.assignment().category().orElseThrow().key()).isEqualTo("sales");
            assertThat(duplicate.transaction().description()).isEqualTo("Venta Caja 1");
        });
    }

    @Test
    void duplicateExternalReferenceReturnsExistingStatusPartitionUnchanged() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var manualReview = record(profileId, CashflowMovementStatus.MANUAL_REVIEW, null, "Manual Caja 1", "manual-001", null);
        var rejected = record(profileId, CashflowMovementStatus.REJECTED, null, null, "rejected-001", "SENSITIVE_IDENTIFIER_REJECTED");
        historyPort.existing.add(manualReview);
        historyPort.existing.add(rejected);
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(
                        item(transaction("Manual cambiado"), "manual-001"),
                        item(transaction("Rechazado cambiado"), "rejected-001")
                )
        ));

        assertThat(historyPort.drafts).isEmpty();
        assertThat(result.manualReview()).singleElement()
                .extracting(CashflowIngestionService.ManualReviewTransaction::movementId)
                .isEqualTo(manualReview.id());
        assertThat(result.rejected()).singleElement()
                .extracting(CashflowIngestionService.RejectedTransaction::movementId)
                .isEqualTo(rejected.id());
    }

    @Test
    void blankExternalReferenceIsTreatedAsOmittedWithFingerprintLookup() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        historyPort.existing.add(record(profileId, CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 1", "   ", null));
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Venta Caja 1"), "   "))
        ));

        assertThat(historyPort.lookupCalls).hasValue(1);
        assertThat(historyPort.drafts).singleElement()
                .extracting(CashflowMovementDraft::sourceReference)
                .asString()
                .startsWith("fp:v1:");
        assertThat(result.categorized()).singleElement()
                .extracting(CashflowIngestionService.CategorizedTransaction::transaction)
                .satisfies(transaction -> assertThat(transaction.description()).isEqualTo("Venta Caja 1"));
    }

    @Test
    void repeatedNoReferenceTransactionReturnsExistingMovementWithoutInsert() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );
        var transaction = transaction("Venta Caja 1");
        var first = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(profileId, List.of(item(transaction))));
        historyPort.existing.add(recordFromDraft(historyPort.drafts.getFirst(), first.categorized().getFirst().movementId()));
        historyPort.drafts.clear();

        var second = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(profileId, List.of(item(transaction))));

        assertThat(historyPort.lookupCalls).hasValue(2);
        assertThat(historyPort.drafts).isEmpty();
        assertThat(second.categorized()).singleElement()
                .extracting(CashflowIngestionService.CategorizedTransaction::movementId)
                .isEqualTo(first.categorized().getFirst().movementId());
    }

    @Test
    void materiallyDifferentNoReferenceFieldsCreateNewMovements() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );

        service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Venta Caja 1", BigDecimal.valueOf(1000))), item(transaction("Venta Caja 1", BigDecimal.valueOf(1001))))
        ));

        assertThat(historyPort.drafts)
                .extracting(CashflowMovementDraft::sourceReference)
                .hasSize(2)
                .allSatisfy(reference -> assertThat(reference).asString().startsWith("fp:v1:"))
                .doesNotHaveDuplicates();
    }

    @Test
    void sameNoReferenceFingerprintIsScopedByProfile() {
        var profileId = new ProfileId("retail-cl");
        var otherProfileId = new ProfileId("other-retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var otherProfile = new VerticalProfile(otherProfileId, "Other Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> id.equals(profileId) ? Optional.of(profile) : Optional.of(otherProfile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );
        var transaction = transaction("Venta Caja 1");

        service.ingest(new CashflowIngestionService.CashflowIngestionCommand(profileId, List.of(item(transaction))));
        service.ingest(new CashflowIngestionService.CashflowIngestionCommand(otherProfileId, List.of(item(transaction))));

        assertThat(historyPort.drafts).hasSize(2);
        assertThat(historyPort.drafts)
                .extracting(CashflowMovementDraft::profileId)
                .containsExactly(profileId, otherProfileId);
        assertThat(historyPort.drafts)
                .extracting(CashflowMovementDraft::sourceReference)
                .doesNotHaveDuplicates();
    }

    @Test
    void identicalNoReferenceCashTransactionsDeduplicateAsMvpLimitation() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );
        var transaction = transaction("Venta Caja 1");

        var first = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(profileId, List.of(item(transaction))));
        historyPort.existing.add(recordFromDraft(historyPort.drafts.getFirst(), first.categorized().getFirst().movementId()));
        historyPort.drafts.clear();
        service.ingest(new CashflowIngestionService.CashflowIngestionCommand(profileId, List.of(item(transaction))));

        assertThat(historyPort.drafts).isEmpty();
        assertThat(historyPort.existing).hasSize(1);
    }

    @Test
    void sameExternalReferenceForDifferentProfileInsertsNewMovement() {
        var profileId = new ProfileId("retail-cl");
        var otherProfileId = new ProfileId("other-retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        historyPort.existing.add(record(otherProfileId, CashflowMovementStatus.PROJECTABLE, "sales", "Venta Caja 1", "batch-001", null));
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of()),
                historyPort
        );

        service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Venta Caja 1"), "batch-001"))
        ));

        assertThat(historyPort.drafts).singleElement()
                .extracting(CashflowMovementDraft::sourceReference)
                .isEqualTo("batch-001");
    }

    @Test
    void sensitiveExternalReferenceUsesFingerprintFallbackWithoutPersistingSensitiveReference() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var categorizationCalls = new AtomicInteger();
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> {
                    categorizationCalls.incrementAndGet();
                    return new CategoryAssignment(Optional.of(category), false);
                },
                new SensitiveDataPolicy(List.of("rut")),
                historyPort
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction("Venta Caja 1"), "rut-123"))
        ));

        assertThat(categorizationCalls).hasValue(0);
        assertThat(historyPort.lookupCalls).hasValue(1);
        assertThat(historyPort.drafts).singleElement().satisfies(draft -> {
            assertThat(draft.status()).isEqualTo(CashflowMovementStatus.REJECTED);
            assertThat(draft.sourceReference()).startsWith("fp:v1:");
            assertThat(draft.sourceReference()).doesNotContain("rut-123");
        });
        assertThat(result.rejected()).hasSize(1);
    }

    @Test
    void repeatedSensitiveExternalReferenceReturnsExistingRejectedMovementWithoutDuplicateInsert() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var historyPort = new RecordingHistoryPort();
        var service = new CashflowIngestionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                (transaction, loadedProfile) -> new CategoryAssignment(Optional.of(category), false),
                new SensitiveDataPolicy(List.of("rut")),
                historyPort
        );
        var transaction = transaction("Venta Caja 1");

        var first = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction, "rut-123"))
        ));
        var firstDraft = historyPort.drafts.getFirst();
        historyPort.existing.add(recordFromDraft(firstDraft, first.rejected().getFirst().movementId()));
        historyPort.drafts.clear();

        var replay = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                profileId,
                List.of(item(transaction, "rut-123"))
        ));

        assertThat(historyPort.drafts).isEmpty();
        assertThat(replay.rejected()).singleElement().satisfies(rejected -> {
            assertThat(rejected.movementId()).isEqualTo(first.rejected().getFirst().movementId());
            assertThat(rejected.reasonCode()).isEqualTo("SENSITIVE_IDENTIFIER_REJECTED");
        });
        assertThat(firstDraft.sourceReference()).startsWith("fp:v1:");
        assertThat(firstDraft.sourceReference()).doesNotContain("rut-123");
    }

    private static Transaction transaction(String description) {
        return transaction(description, BigDecimal.valueOf(1000));
    }

    private static Transaction transaction(String description, BigDecimal amount) {
        return new Transaction(description, amount, Currency.getInstance("CLP"), LocalDate.now());
    }

    private static CashflowIngestionService.CashflowIngestionCommand.IngestionItem item(Transaction transaction) {
        return item(transaction, null);
    }

    private static CashflowIngestionService.CashflowIngestionCommand.IngestionItem item(Transaction transaction, String externalReference) {
        return new CashflowIngestionService.CashflowIngestionCommand.IngestionItem(transaction, externalReference);
    }

    private static CashflowMovementRecord record(
            ProfileId profileId,
            CashflowMovementStatus status,
            String categoryKey,
            String safeDescription,
            String sourceReference,
            String rejectionReasonCode
    ) {
        var now = Instant.now();
        return new CashflowMovementRecord(
                UUID.randomUUID(),
                profileId,
                BigDecimal.valueOf(1000),
                Currency.getInstance("CLP"),
                LocalDate.now(),
                status,
                categoryKey,
                safeDescription,
                sourceReference,
                rejectionReasonCode,
                null,
                now,
                now
        );
    }

    private static CashflowMovementRecord recordFromDraft(CashflowMovementDraft draft, UUID id) {
        var now = Instant.now();
        return new CashflowMovementRecord(
                id,
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
        );
    }

    private static final class RecordingHistoryPort implements CashflowMovementHistoryPort {
        private final List<CashflowMovementDraft> drafts = new ArrayList<>();
        private final List<CashflowMovementRecord> existing = new ArrayList<>();
        private final AtomicInteger lookupCalls = new AtomicInteger();

        @Override
        public List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts) {
            this.drafts.addAll(drafts);
            var now = Instant.now();
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
                    .toList();
        }

        @Override
        public Optional<CashflowMovementRecord> findById(UUID movementId) {
            return Optional.empty();
        }

        @Override
        public Optional<CashflowMovementRecord> findBySourceReference(ProfileId profileId, String sourceReference) {
            lookupCalls.incrementAndGet();
            return existing.stream()
                    .filter(record -> record.profileId().equals(profileId))
                    .filter(record -> sourceReference.equals(record.sourceReference()))
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
