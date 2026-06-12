package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PharmacyCashflowServiceTest {

    @Test
    void categorizesAcceptedTransactionsAndRejectsSensitiveIdentifiers() {
        var profileId = new ProfileId("retail-cl");
        var category = new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW);
        var profile = new VerticalProfile(profileId, "Retail", List.of(), List.of(category), List.of());
        var profileService = new VerticalProfileService(id -> Optional.of(profile));
        CashflowCategorizationPort categorizationPort = (transaction, loadedProfile) ->
                new CategoryAssignment(Optional.of(category), false);
        var service = new PharmacyCashflowService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of("blocked-token"))
        );
        var accepted = transaction("Venta Caja 1");
        var rejected = transaction("Venta Caja 1 blocked-token");

        var result = service.ingest(new PharmacyCashflowService.CashflowIngestionCommand(
                profileId,
                List.of(accepted, rejected)
        ));

        assertThat(result.categorized()).singleElement()
                .extracting(PharmacyCashflowService.CategorizedTransaction::transaction)
                .isEqualTo(accepted);
        assertThat(result.manualReview()).isEmpty();
        assertThat(result.rejected()).singleElement()
                .extracting(PharmacyCashflowService.RejectedTransaction::transaction)
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
        var service = new PharmacyCashflowService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of())
        );

        var result = service.ingest(new PharmacyCashflowService.CashflowIngestionCommand(
                profileId,
                List.of(transaction("Unmatched movement"))
        ));

        assertThat(result.categorized()).isEmpty();
        assertThat(result.manualReview()).singleElement()
                .extracting(PharmacyCashflowService.ManualReviewTransaction::assignment)
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
        var service = new PharmacyCashflowService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of("blocked-token"))
        );

        var result = service.ingest(new PharmacyCashflowService.CashflowIngestionCommand(
                profileId,
                List.of(transaction("Venta Caja 1 blocked-token"))
        ));

        assertThat(categorizationCalls).hasValue(0);
        assertThat(result.categorized()).isEmpty();
        assertThat(result.manualReview()).isEmpty();
        assertThat(result.rejected()).hasSize(1);
    }

    private static Transaction transaction(String description) {
        return new Transaction(description, BigDecimal.valueOf(1000), Currency.getInstance("CLP"), LocalDate.now());
    }
}
