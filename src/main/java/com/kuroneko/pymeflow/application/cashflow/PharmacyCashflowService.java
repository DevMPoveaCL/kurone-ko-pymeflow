package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.ArrayList;
import java.util.List;

public final class PharmacyCashflowService {
    private static final String SENSITIVE_IDENTIFIER_REJECTED = "SENSITIVE_IDENTIFIER_REJECTED";

    private final VerticalProfileService verticalProfileService;
    private final CashflowCategorizationPort cashflowCategorizationPort;
    private final SensitiveDataPolicy sensitiveDataPolicy;

    public PharmacyCashflowService(
            VerticalProfileService verticalProfileService,
            CashflowCategorizationPort cashflowCategorizationPort,
            SensitiveDataPolicy sensitiveDataPolicy
    ) {
        this.verticalProfileService = verticalProfileService;
        this.cashflowCategorizationPort = cashflowCategorizationPort;
        this.sensitiveDataPolicy = sensitiveDataPolicy;
    }

    public CashflowIngestionResult ingest(CashflowIngestionCommand command) {
        var profile = verticalProfileService.loadProfile(command.profileId());
        var categorized = new ArrayList<CategorizedTransaction>();
        var manualReview = new ArrayList<ManualReviewTransaction>();
        var rejected = new ArrayList<RejectedTransaction>();

        for (Transaction transaction : command.transactions()) {
            if (sensitiveDataPolicy.rejects(transaction)) {
                rejected.add(new RejectedTransaction(transaction, SENSITIVE_IDENTIFIER_REJECTED));
                continue;
            }

            var assignment = cashflowCategorizationPort.categorize(transaction, profile);
            if (assignment.category().isPresent() && !assignment.requiresManualReview()) {
                categorized.add(new CategorizedTransaction(transaction, assignment));
            } else {
                manualReview.add(new ManualReviewTransaction(transaction, assignment));
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

    public record CategorizedTransaction(Transaction transaction, CategoryAssignment assignment) {
        public CategorizedTransaction {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (assignment == null) {
                throw new IllegalArgumentException("Category assignment is required");
            }
        }
    }

    public record ManualReviewTransaction(Transaction transaction, CategoryAssignment assignment) {
        public ManualReviewTransaction {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (assignment == null) {
                throw new IllegalArgumentException("Category assignment is required");
            }
        }
    }

    public record RejectedTransaction(Transaction transaction, String reasonCode) {
        public RejectedTransaction {
            if (transaction == null) {
                throw new IllegalArgumentException("Transaction is required");
            }
            if (reasonCode == null || reasonCode.isBlank()) {
                throw new IllegalArgumentException("Reason code is required");
            }
        }
    }
}
