package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashflowMovementDraftTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 19);

    @Test
    void storesRequiredMovementDirection() {
        var draft = draft(TransactionDirection.DEBIT, BigDecimal.valueOf(15_000));

        assertThat(draft.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(draft.amount()).isEqualByComparingTo("15000");
    }

    @Test
    void rejectsNullDirection() {
        assertThatThrownBy(() -> draft(null, BigDecimal.valueOf(15_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Direction is required");
    }

    @Test
    void stillRejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> draft(TransactionDirection.CREDIT, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
    }

    @Test
    void compatibilityConstructorDefaultsDirectionToCredit() {
        var draft = new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                CashflowMovementStatus.MANUAL_REVIEW,
                null,
                "Sale",
                "batch-1",
                null
        );

        assertThat(draft.direction()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(draft.status()).isEqualTo(CashflowMovementStatus.MANUAL_REVIEW);
    }

    @Test
    void normalizesOptionalTextFields() {
        var draft = new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.CREDIT,
                CashflowMovementStatus.REJECTED,
                null,
                "  Safe sale  ",
                "  batch-1  ",
                "  policy-blocked  "
        );

        assertThat(draft.categoryKeyOptional()).isEmpty();
        assertThat(draft.safeDescriptionOptional()).contains("Safe sale");
        assertThat(draft.sourceReferenceOptional()).contains("batch-1");
        assertThat(draft.rejectionReasonCodeOptional()).contains("policy-blocked");
    }

    @Test
    void validatesCategoryPresenceByStatus() {
        assertThatThrownBy(() -> new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.CREDIT,
                CashflowMovementStatus.PROJECTABLE,
                null,
                "Sale",
                "batch-1",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category key is required for projectable movements");
        assertThatThrownBy(() -> new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.CREDIT,
                CashflowMovementStatus.MANUAL_REVIEW,
                "sales",
                "Sale",
                "batch-1",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category key is only allowed for projectable movements");
    }

    @Test
    void rejectsMissingRequiredCoreFields() {
        assertThatThrownBy(() -> new CashflowMovementDraft(null, BigDecimal.ONE, CLP, DATE, TransactionDirection.CREDIT,
                CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile id is required");
        assertThatThrownBy(() -> new CashflowMovementDraft(PROFILE_ID, BigDecimal.ONE, null, DATE, TransactionDirection.CREDIT,
                CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
        assertThatThrownBy(() -> new CashflowMovementDraft(PROFILE_ID, BigDecimal.ONE, CLP, null, TransactionDirection.CREDIT,
                CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date is required");
        assertThatThrownBy(() -> new CashflowMovementDraft(PROFILE_ID, BigDecimal.ONE, CLP, DATE, TransactionDirection.CREDIT,
                null, "sales", "Sale", "batch-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status is required");
    }

    @Test
    void rejectsOverlongOptionalTextFields() {
        assertThatThrownBy(() -> new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                TransactionDirection.CREDIT,
                CashflowMovementStatus.PROJECTABLE,
                "x".repeat(81),
                "Sale",
                "batch-1",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category key is too long");
    }

    private static CashflowMovementDraft draft(TransactionDirection direction, BigDecimal amount) {
        return new CashflowMovementDraft(
                PROFILE_ID,
                amount,
                CLP,
                DATE,
                direction,
                CashflowMovementStatus.PROJECTABLE,
                "sales",
                "Sale",
                "batch-1",
                null
        );
    }
}
