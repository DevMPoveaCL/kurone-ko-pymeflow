package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CashflowIngestionService {
    private static final String SENSITIVE_IDENTIFIER_REJECTED = "SENSITIVE_IDENTIFIER_REJECTED";

    private final VerticalProfileService verticalProfileService;
    private final CashflowCategorizationPort cashflowCategorizationPort;
    private final SensitiveDataPolicy sensitiveDataPolicy;
    private final CashflowMovementHistoryPort cashflowMovementHistoryPort;

    public CashflowIngestionService(
            VerticalProfileService verticalProfileService,
            CashflowCategorizationPort cashflowCategorizationPort,
            SensitiveDataPolicy sensitiveDataPolicy,
            CashflowMovementHistoryPort cashflowMovementHistoryPort
    ) {
        this.verticalProfileService = verticalProfileService;
        this.cashflowCategorizationPort = cashflowCategorizationPort;
        this.sensitiveDataPolicy = sensitiveDataPolicy;
        this.cashflowMovementHistoryPort = cashflowMovementHistoryPort;
    }

    public CashflowIngestionResult ingest(CashflowIngestionCommand command) {
        var profile = verticalProfileService.loadProfile(command.profileId());
        var outcomes = new ArrayList<IngestionOutcome>();
        var resultItems = new ArrayList<ResultItem>();

        for (var item : command.items()) {
            var transaction = item.transaction();
            var externalReference = item.externalReference();
            var sourceReference = externalReference;
            var sensitiveExternalReference = externalReference != null && sensitiveDataPolicy.rejectsText(externalReference);

            if (sensitiveExternalReference) {
                sourceReference = TransactionFingerprint.compute(command.profileId(), transaction);
            }

            if (sourceReference == null) {
                sourceReference = TransactionFingerprint.compute(command.profileId(), transaction);
            }

            var existing = cashflowMovementHistoryPort.findBySourceReference(command.profileId(), sourceReference);
            if (existing.isPresent()) {
                resultItems.add(ResultItem.existing(existing.orElseThrow(), transaction, profile, sourceReference));
                continue;
            }

            if (sensitiveExternalReference) {
                outcomes.add(IngestionOutcome.rejected(transaction, command.profileId(), sourceReference, SENSITIVE_IDENTIFIER_REJECTED));
                continue;
            }

            if (sensitiveDataPolicy.rejects(transaction)) {
                outcomes.add(IngestionOutcome.rejected(transaction, command.profileId(), sourceReference, SENSITIVE_IDENTIFIER_REJECTED));
                continue;
            }

            var assignment = cashflowCategorizationPort.categorize(transaction, profile);
            if (assignment.category().isPresent() && !assignment.requiresManualReview()) {
                outcomes.add(IngestionOutcome.categorized(transaction, command.profileId(), sourceReference, assignment));
            } else {
                outcomes.add(IngestionOutcome.manualReview(transaction, command.profileId(), sourceReference, assignment));
            }
        }

        var records = cashflowMovementHistoryPort.saveAll(outcomes.stream()
                .map(IngestionOutcome::draft)
                .toList());

        for (int index = 0; index < outcomes.size(); index++) {
            resultItems.add(ResultItem.saved(outcomes.get(index), records.get(index)));
        }

        var categorized = new ArrayList<CategorizedTransaction>();
        var manualReview = new ArrayList<ManualReviewTransaction>();
        var rejected = new ArrayList<RejectedTransaction>();
        for (var resultItem : resultItems) {
            if (resultItem.status() == CashflowMovementStatus.PROJECTABLE) {
                categorized.add(new CategorizedTransaction(resultItem.movementId(), resultItem.transaction(), resultItem.assignment(), resultItem.sourceReference()));
            } else if (resultItem.status() == CashflowMovementStatus.MANUAL_REVIEW) {
                manualReview.add(new ManualReviewTransaction(resultItem.movementId(), resultItem.transaction(), resultItem.assignment(), resultItem.sourceReference()));
            } else {
                rejected.add(new RejectedTransaction(resultItem.movementId(), resultItem.transaction(), resultItem.reasonCode(), resultItem.sourceReference()));
            }
        }

        return new CashflowIngestionResult(categorized, manualReview, rejected);
    }

    public record CashflowIngestionCommand(ProfileId profileId, List<IngestionItem> items) {
        public CashflowIngestionCommand {
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            items = List.copyOf(items == null ? List.of() : items);
        }

        public List<Transaction> transactions() {
            return items.stream().map(IngestionItem::transaction).toList();
        }

        public record IngestionItem(Transaction transaction, String externalReference) {
            public IngestionItem {
                if (transaction == null) {
                    throw new IllegalArgumentException("Transaction is required");
                }
                externalReference = (externalReference == null || externalReference.isBlank())
                        ? null
                        : externalReference.trim();
            }
        }
    }

    public record CashflowIngestionResult(
            List<CategorizedTransaction> categorized,
            List<ManualReviewTransaction> manualReview,
            List<RejectedTransaction> rejected
    ) {
        public CashflowIngestionResult {
            categorized = List.copyOf(categorized == null ? List.of() : categorized);
            manualReview = List.copyOf(manualReview == null ? List.of() : manualReview);
            rejected = List.copyOf(rejected == null ? List.of() : rejected);
        }
    }

    public record CategorizedTransaction(UUID movementId, Transaction transaction, CategoryAssignment assignment, String sourceReference) {
        public CategorizedTransaction(Transaction transaction, CategoryAssignment assignment) {
            this(null, transaction, assignment);
        }

        public CategorizedTransaction(UUID movementId, Transaction transaction, CategoryAssignment assignment) {
            this(movementId, transaction, assignment, null);
        }

        public CategorizedTransaction {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (assignment == null) {
                throw new IllegalArgumentException("Category assignment is required");
            }
        }
    }

    public record ManualReviewTransaction(UUID movementId, Transaction transaction, CategoryAssignment assignment, String sourceReference) {
        public ManualReviewTransaction(Transaction transaction, CategoryAssignment assignment) {
            this(null, transaction, assignment);
        }

        public ManualReviewTransaction(UUID movementId, Transaction transaction, CategoryAssignment assignment) {
            this(movementId, transaction, assignment, null);
        }

        public ManualReviewTransaction {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (assignment == null) {
                throw new IllegalArgumentException("Category assignment is required");
            }
        }
    }

    public record RejectedTransaction(UUID movementId, Transaction transaction, String reasonCode, String sourceReference) {
        public RejectedTransaction(Transaction transaction, String reasonCode) {
            this(null, transaction, reasonCode);
        }

        public RejectedTransaction(UUID movementId, Transaction transaction, String reasonCode) {
            this(movementId, transaction, reasonCode, null);
        }

        public RejectedTransaction {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (reasonCode == null || reasonCode.isBlank()) {
                throw new IllegalArgumentException("Reason code is required");
            }
        }
    }

    private record IngestionOutcome(
            CashflowMovementStatus status,
            Transaction transaction,
            CategoryAssignment assignment,
            String reasonCode,
            CashflowMovementDraft draft
    ) {
        static IngestionOutcome categorized(Transaction transaction, ProfileId profileId, String sourceReference, CategoryAssignment assignment) {
            var categoryKey = assignment.category()
                    .orElseThrow(() -> new IllegalArgumentException("Category is required"))
                    .key();
            return new IngestionOutcome(
                    CashflowMovementStatus.PROJECTABLE,
                    transaction,
                    assignment,
                    null,
                    new CashflowMovementDraft(
                            profileId,
                            transaction.amount(),
                            transaction.currency(),
                            transaction.bookedAt(),
                            CashflowMovementStatus.PROJECTABLE,
                            categoryKey,
                            transaction.description(),
                            sourceReference,
                            null
                    )
            );
        }

        static IngestionOutcome manualReview(Transaction transaction, ProfileId profileId, String sourceReference, CategoryAssignment assignment) {
            return new IngestionOutcome(
                    CashflowMovementStatus.MANUAL_REVIEW,
                    transaction,
                    assignment,
                    null,
                    new CashflowMovementDraft(
                            profileId,
                            transaction.amount(),
                            transaction.currency(),
                            transaction.bookedAt(),
                            CashflowMovementStatus.MANUAL_REVIEW,
                            null,
                            transaction.description(),
                            sourceReference,
                            null
                    )
            );
        }

        static IngestionOutcome rejected(Transaction transaction, ProfileId profileId, String sourceReference, String reasonCode) {
            return new IngestionOutcome(
                    CashflowMovementStatus.REJECTED,
                    transaction,
                    new CategoryAssignment(java.util.Optional.empty(), false),
                    reasonCode,
                    new CashflowMovementDraft(
                            profileId,
                            transaction.amount(),
                            transaction.currency(),
                            transaction.bookedAt(),
                            CashflowMovementStatus.REJECTED,
                            null,
                            null,
                            sourceReference,
                            reasonCode
                    )
            );
        }
    }

    private record ResultItem(
            UUID movementId,
            CashflowMovementStatus status,
            Transaction transaction,
            CategoryAssignment assignment,
            String reasonCode,
            String sourceReference
    ) {
        static ResultItem saved(IngestionOutcome outcome, CashflowMovementRecord record) {
            return new ResultItem(record.id(), outcome.status(), outcome.transaction(), outcome.assignment(), outcome.reasonCode(), outcome.draft().sourceReference());
        }

        static ResultItem existing(CashflowMovementRecord record, Transaction fallbackTransaction, VerticalProfile profile, String sourceReference) {
            return new ResultItem(
                    record.id(),
                    record.status(),
                    transactionFrom(record, fallbackTransaction),
                    assignmentFrom(record, profile),
                    record.rejectionReasonCode(),
                    sourceReference
            );
        }

        private static Transaction transactionFrom(CashflowMovementRecord record, Transaction fallbackTransaction) {
            var description = record.safeDescription() == null || record.safeDescription().isBlank()
                    ? fallbackTransaction.description()
                    : record.safeDescription();
            return new Transaction(description, record.amount(), record.currency(), record.date());
        }

        private static CategoryAssignment assignmentFrom(CashflowMovementRecord record, VerticalProfile profile) {
            if (record.status() == CashflowMovementStatus.PROJECTABLE) {
                return new CategoryAssignment(categoryFor(record, profile), false);
            }
            return new CategoryAssignment(Optional.empty(), record.status() == CashflowMovementStatus.MANUAL_REVIEW);
        }

        private static Optional<CashflowCategory> categoryFor(CashflowMovementRecord record, VerticalProfile profile) {
            return profile.categories().stream()
                    .filter(category -> category.key().equals(record.categoryKey()))
                    .findFirst();
        }
    }
}
