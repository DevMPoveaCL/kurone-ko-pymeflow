package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.ArrayList;
import java.util.List;
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

        for (Transaction transaction : command.transactions()) {
            if (sensitiveDataPolicy.rejects(transaction)) {
                outcomes.add(IngestionOutcome.rejected(transaction, command.profileId(), SENSITIVE_IDENTIFIER_REJECTED));
                continue;
            }

            var assignment = cashflowCategorizationPort.categorize(transaction, profile);
            if (assignment.category().isPresent() && !assignment.requiresManualReview()) {
                outcomes.add(IngestionOutcome.categorized(transaction, command.profileId(), assignment));
            } else {
                outcomes.add(IngestionOutcome.manualReview(transaction, command.profileId(), assignment));
            }
        }

        var records = cashflowMovementHistoryPort.saveAll(outcomes.stream()
                .map(IngestionOutcome::draft)
                .toList());

        var categorized = new ArrayList<CategorizedTransaction>();
        var manualReview = new ArrayList<ManualReviewTransaction>();
        var rejected = new ArrayList<RejectedTransaction>();
        for (int index = 0; index < outcomes.size(); index++) {
            var outcome = outcomes.get(index);
            var record = records.get(index);
            if (outcome.status() == CashflowMovementStatus.PROJECTABLE) {
                categorized.add(new CategorizedTransaction(record.id(), outcome.transaction(), outcome.assignment()));
            } else if (outcome.status() == CashflowMovementStatus.MANUAL_REVIEW) {
                manualReview.add(new ManualReviewTransaction(record.id(), outcome.transaction(), outcome.assignment()));
            } else {
                rejected.add(new RejectedTransaction(record.id(), outcome.transaction(), outcome.reasonCode()));
            }
        }

        return new CashflowIngestionResult(categorized, manualReview, rejected);
    }

    public record CashflowIngestionCommand(ProfileId profileId, List<Transaction> transactions) {
        public CashflowIngestionCommand {
            if (profileId == null) {
                throw new IllegalArgumentException("Profile id is required");
            }
            transactions = List.copyOf(transactions == null ? List.of() : transactions);
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

    public record CategorizedTransaction(UUID movementId, Transaction transaction, CategoryAssignment assignment) {
        public CategorizedTransaction(Transaction transaction, CategoryAssignment assignment) {
            this(null, transaction, assignment);
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

    public record ManualReviewTransaction(UUID movementId, Transaction transaction, CategoryAssignment assignment) {
        public ManualReviewTransaction(Transaction transaction, CategoryAssignment assignment) {
            this(null, transaction, assignment);
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

    public record RejectedTransaction(UUID movementId, Transaction transaction, String reasonCode) {
        public RejectedTransaction(Transaction transaction, String reasonCode) {
            this(null, transaction, reasonCode);
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
        static IngestionOutcome categorized(Transaction transaction, ProfileId profileId, CategoryAssignment assignment) {
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
                            null,
                            null
                    )
            );
        }

        static IngestionOutcome manualReview(Transaction transaction, ProfileId profileId, CategoryAssignment assignment) {
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
                            null,
                            null
                    )
            );
        }

        static IngestionOutcome rejected(Transaction transaction, ProfileId profileId, String reasonCode) {
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
                            null,
                            reasonCode
                    )
            );
        }
    }
}
