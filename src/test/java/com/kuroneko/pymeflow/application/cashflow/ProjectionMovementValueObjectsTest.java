package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectionMovementValueObjectsTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 19);
    private static final UUID MOVEMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void projectionReadyTransactionCarriesDirectionIntoProjectionTransaction() {
        var projectionReady = new ProjectionReadyCashflowTransaction(
                MOVEMENT_ID,
                "sales",
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.DEBIT,
                CashflowMovementStatus.PROJECTABLE
        );

        assertThat(projectionReady.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(projectionReady.toProjectionTransaction()).isEqualTo(new ProjectedCashflowTransaction(
                "sales",
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.DEBIT
        ));
    }

    @Test
    void projectionReadyTransactionDefaultsCompatibilityConstructorToCredit() {
        var projectionReady = new ProjectionReadyCashflowTransaction(
                MOVEMENT_ID,
                "sales",
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                CashflowMovementStatus.PROJECTABLE
        );

        assertThat(projectionReady.direction()).isEqualTo(TransactionDirection.CREDIT);
    }

    @Test
    void projectionReadyTransactionRejectsInvalidInputs() {
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(null, "sales", BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Movement id is required");
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(MOVEMENT_ID, " ", BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category key is required");
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(MOVEMENT_ID, "sales", BigDecimal.ZERO, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(MOVEMENT_ID, "sales", BigDecimal.ONE, null, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(MOVEMENT_ID, "sales", BigDecimal.ONE, CLP, null,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date is required");
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(MOVEMENT_ID, "sales", BigDecimal.ONE, CLP, DATE,
                null, CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Direction is required");
        assertThatThrownBy(() -> new ProjectionReadyCashflowTransaction(MOVEMENT_ID, "sales", BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.MANUAL_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Projection-ready transaction must be projectable");
    }

    @Test
    void projectedTransactionDefaultsCompatibilityConstructorToCreditAndAllowsZeroAmount() {
        var projected = new ProjectedCashflowTransaction("sales", BigDecimal.ZERO, CLP, DATE);

        assertThat(projected.direction()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(projected.amount()).isEqualByComparingTo("0");
    }

    @Test
    void projectedTransactionRejectsInvalidInputs() {
        assertThatThrownBy(() -> new ProjectedCashflowTransaction(" ", BigDecimal.ONE, CLP, DATE, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category key is required");
        assertThatThrownBy(() -> new ProjectedCashflowTransaction("sales", BigDecimal.valueOf(-1), CLP, DATE, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be zero or positive");
        assertThatThrownBy(() -> new ProjectedCashflowTransaction("sales", BigDecimal.ONE, null, DATE, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
        assertThatThrownBy(() -> new ProjectedCashflowTransaction("sales", BigDecimal.ONE, CLP, null, TransactionDirection.CREDIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date is required");
        assertThatThrownBy(() -> new ProjectedCashflowTransaction("sales", BigDecimal.ONE, CLP, DATE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Direction is required");
    }

    @Test
    void pendingManualReviewMovementCarriesDirectionFromRecord() {
        var record = new CashflowMovementRecord(
                MOVEMENT_ID,
                new ProfileId("retail-cl"),
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.DEBIT,
                CashflowMovementStatus.MANUAL_REVIEW,
                null,
                "Safe sale",
                "batch-1",
                null,
                null,
                Instant.parse("2026-06-19T00:00:00Z"),
                Instant.parse("2026-06-19T00:00:00Z")
        );

        var pending = PendingManualReviewMovement.from(record);

        assertThat(pending.movementId()).isEqualTo(MOVEMENT_ID);
        assertThat(pending.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(pending.description()).isEqualTo("Safe sale");
        assertThat(pending.sourceReference()).isEqualTo("batch-1");
    }

    @Test
    void pendingManualReviewMovementDefaultsCompatibilityConstructorToCredit() {
        var pending = new PendingManualReviewMovement(
                MOVEMENT_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                "Safe sale",
                "batch-1",
                CashflowMovementStatus.MANUAL_REVIEW
        );

        assertThat(pending.direction()).isEqualTo(TransactionDirection.CREDIT);
    }

    @Test
    void pendingManualReviewMovementRejectsInvalidInputs() {
        assertThatThrownBy(() -> new PendingManualReviewMovement(null, BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, "Safe sale", "batch-1", CashflowMovementStatus.MANUAL_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Movement id is required");
        assertThatThrownBy(() -> new PendingManualReviewMovement(MOVEMENT_ID, BigDecimal.ZERO, CLP, DATE,
                TransactionDirection.CREDIT, "Safe sale", "batch-1", CashflowMovementStatus.MANUAL_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
        assertThatThrownBy(() -> new PendingManualReviewMovement(MOVEMENT_ID, BigDecimal.ONE, null, DATE,
                TransactionDirection.CREDIT, "Safe sale", "batch-1", CashflowMovementStatus.MANUAL_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
        assertThatThrownBy(() -> new PendingManualReviewMovement(MOVEMENT_ID, BigDecimal.ONE, CLP, null,
                TransactionDirection.CREDIT, "Safe sale", "batch-1", CashflowMovementStatus.MANUAL_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date is required");
        assertThatThrownBy(() -> new PendingManualReviewMovement(MOVEMENT_ID, BigDecimal.ONE, CLP, DATE,
                null, "Safe sale", "batch-1", CashflowMovementStatus.MANUAL_REVIEW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Direction is required");
        assertThatThrownBy(() -> new PendingManualReviewMovement(MOVEMENT_ID, BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, "Safe sale", "batch-1", CashflowMovementStatus.PROJECTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pending movement must be in manual review");
    }
}
