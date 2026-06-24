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

class CashflowMovementRecordTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 19);
    private static final Instant NOW = Instant.parse("2026-06-19T00:00:00Z");

    @Test
    void storesMovementDirectionFromDraft() {
        var draft = draft(TransactionDirection.DEBIT);
        var record = recordFrom(draft);

        assertThat(record.direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(record.amount()).isEqualByComparingTo(draft.amount());
    }

    @Test
    void rejectsNullDirection() {
        assertThatThrownBy(() -> record(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Direction is required");
    }

    @Test
    void compatibilityConstructorDefaultsDirectionToCredit() {
        var record = new CashflowMovementRecord(
                UUID.randomUUID(),
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                CashflowMovementStatus.PROJECTABLE,
                "sales",
                "Sale",
                "batch-1",
                null,
                null,
                NOW,
                NOW
        );

        assertThat(record.direction()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(record.categoryKeyOptional()).contains("sales");
        assertThat(record.safeDescriptionOptional()).contains("Sale");
        assertThat(record.sourceReferenceOptional()).contains("batch-1");
    }

    @Test
    void rejectsMissingCoreFields() {
        assertThatThrownBy(() -> new CashflowMovementRecord(null, PROFILE_ID, BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Movement id is required");
        assertThatThrownBy(() -> new CashflowMovementRecord(UUID.randomUUID(), null, BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile id is required");
        assertThatThrownBy(() -> new CashflowMovementRecord(UUID.randomUUID(), PROFILE_ID, BigDecimal.ZERO, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
        assertThatThrownBy(() -> new CashflowMovementRecord(UUID.randomUUID(), PROFILE_ID, BigDecimal.ONE, null, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency is required");
        assertThatThrownBy(() -> new CashflowMovementRecord(UUID.randomUUID(), PROFILE_ID, BigDecimal.ONE, CLP, null,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date is required");
        assertThatThrownBy(() -> new CashflowMovementRecord(UUID.randomUUID(), PROFILE_ID, BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, null, "sales", "Sale", "batch-1", null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status is required");
        assertThatThrownBy(() -> new CashflowMovementRecord(UUID.randomUUID(), PROFILE_ID, BigDecimal.ONE, CLP, DATE,
                TransactionDirection.CREDIT, CashflowMovementStatus.PROJECTABLE, "sales", "Sale", "batch-1", null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamps are required");
    }

    private static CashflowMovementDraft draft(TransactionDirection direction) {
        return new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
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

    private static CashflowMovementRecord recordFrom(CashflowMovementDraft draft) {
        return record(draft.direction());
    }

    private static CashflowMovementRecord record(TransactionDirection direction) {
        return new CashflowMovementRecord(
                UUID.randomUUID(),
                PROFILE_ID,
                BigDecimal.valueOf(15_000),
                CLP,
                DATE,
                direction,
                CashflowMovementStatus.PROJECTABLE,
                "sales",
                "Sale",
                "batch-1",
                null,
                null,
                NOW,
                NOW
        );
    }
}
