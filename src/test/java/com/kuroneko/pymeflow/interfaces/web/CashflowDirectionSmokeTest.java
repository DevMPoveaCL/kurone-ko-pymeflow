package com.kuroneko.pymeflow.interfaces.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CashflowDirectionSmokeTest {
    private static final String PROFILE_ID = "pharmacy-cl";

    @Autowired
    private CashflowBankStatementSimulatedController bankStatementController;

    @Autowired
    private CashflowManualImportController manualImportController;

    @Autowired
    private CashflowHistoryController historyController;

    @Test
    void bankStatementImportPersistsDebitAndCreditAsPositiveAmountsVisibleInHistory() {
        var unique = UUID.randomUUID().toString();
        var debitReference = "SMOKE-BANK-DEBIT-" + unique;
        var creditReference = "SMOKE-BANK-CREDIT-" + unique;
        var request = new CashflowBankStatementSimulatedController.SimulatedBankStatementRequest(
                PROFILE_ID,
                "Smoke bank direction preservation",
                List.of(
                        bankRow(debitReference, "Venta smoke debit", BigDecimal.valueOf(-15000), "Cuenta principal"),
                        bankRow(creditReference, "Venta smoke credit", BigDecimal.valueOf(15000), "Cuenta principal")
                )
        );

        var response = bankStatementController.importSimulated(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.categorized()).hasSize(2);
        assertThat(response.categorized())
                .extracting(item -> item.transaction().movementDirection())
                .containsExactly("DEBIT", "CREDIT");
        assertThat(response.categorized())
                .allSatisfy(item -> assertThat(item.transaction().amount()).isEqualByComparingTo("15000"));

        var movementIds = response.categorized().stream()
                .map(CashflowBankStatementSimulatedController.CategorizedTransactionResponse::movementId)
                .toList();
        var history = historyController.projectionReady(PROFILE_ID, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)).getBody();

        assertThat(history).isNotNull();
        assertThat(history)
                .filteredOn(item -> movementIds.contains(item.movementId()))
                .hasSize(2)
                .extracting(CashflowHistoryController.ProjectionReadyTransactionResponse::movementDirection)
                .containsExactlyInAnyOrder("DEBIT", "CREDIT");
        assertThat(history)
                .filteredOn(item -> movementIds.contains(item.movementId()))
                .allSatisfy(item -> assertThat(item.amount()).isEqualByComparingTo("15000"));
    }

    @Test
    void manualImportDefaultsCreditPersistsExplicitDebitAndKeepsExistingMovementWhenDirectionChanges() {
        var unique = UUID.randomUUID().toString();
        var creditDescription = "Venta smoke manual credit " + unique;
        var debitDescription = "Venta smoke manual debit " + unique;
        var firstRequest = new CashflowManualImportController.ManualImportRequest(
                PROFILE_ID,
                "Smoke manual direction preservation",
                List.of(
                        manualRow(1, creditDescription, BigDecimal.valueOf(18000), null),
                        manualRow(2, debitDescription, BigDecimal.valueOf(19000), "DEBIT")
                )
        );

        var firstResponse = manualImportController.importManual(firstRequest).getBody();

        assertThat(firstResponse).isNotNull();
        assertThat(firstResponse.categorized()).hasSize(2);
        assertThat(firstResponse.categorized())
                .extracting(item -> item.transaction().movementDirection())
                .containsExactly("CREDIT", "DEBIT");

        var replayRequest = new CashflowManualImportController.ManualImportRequest(
                PROFILE_ID,
                "Smoke manual replay direction change",
                List.of(manualRow(1, creditDescription, BigDecimal.valueOf(18000), "DEBIT"))
        );
        var replayResponse = manualImportController.importManual(replayRequest).getBody();

        assertThat(replayResponse).isNotNull();
        assertThat(replayResponse.categorized()).singleElement().satisfies(replayed -> {
            assertThat(replayed.movementId()).isEqualTo(firstResponse.categorized().getFirst().movementId());
            assertThat(replayed.transaction().movementDirection()).isEqualTo("CREDIT");
        });
    }

    private static CashflowBankStatementSimulatedController.SimulatedBankStatementRow bankRow(
            String bankTransactionId,
            String description,
            BigDecimal amount,
            String accountAlias
    ) {
        return new CashflowBankStatementSimulatedController.SimulatedBankStatementRow(
                bankTransactionId,
                "2026-06-15",
                description,
                amount,
                "CLP",
                accountAlias,
                null
        );
    }

    private static CashflowManualImportController.ManualImportRow manualRow(
            int rowNumber,
            String description,
            BigDecimal amount,
            String movementDirection
    ) {
        return new CashflowManualImportController.ManualImportRow(
                rowNumber,
                description,
                amount,
                "CLP",
                "2026-06-16",
                null,
                movementDirection
        );
    }
}
