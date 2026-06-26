package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CockpitProjectionServiceTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final LocalDate START_DATE = LocalDate.of(2026, 6, 1);

    @Test
    void buildsProjectionCommandFromPersistedProjectableHistoryUsingClpCurrencyAndInclusiveHorizon() {
        var historyService = mock(CashflowMovementHistoryService.class);
        var projectionService = mock(CashflowProjectionService.class);
        var service = new CockpitProjectionService(historyService, projectionService);
        var projectedResult = new CashflowProjectionResult(
                List.of(new DailyProjectedBalance(START_DATE, BigDecimal.valueOf(12_000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(112_000))),
                BigDecimal.valueOf(112_000),
                List.of(),
                List.of()
        );
        when(historyService.projectionReady(PROFILE_ID, START_DATE, START_DATE.plusDays(6)))
                .thenReturn(List.of(projectable("sales", 12_000, START_DATE, TransactionDirection.CREDIT)));
        when(projectionService.project(org.mockito.ArgumentMatchers.any())).thenReturn(projectedResult);

        var result = service.projectFromHistory(PROFILE_ID, BigDecimal.valueOf(100_000), START_DATE, 7);

        assertThat(result).isSameAs(projectedResult);
        var command = forClass(CashflowProjectionCommand.class);
        verify(projectionService).project(command.capture());
        assertThat(command.getValue().profileId()).isEqualTo(PROFILE_ID);
        assertThat(command.getValue().openingBalance()).isEqualByComparingTo("100000");
        assertThat(command.getValue().currency()).isEqualTo(CLP);
        assertThat(command.getValue().startDate()).isEqualTo(START_DATE);
        assertThat(command.getValue().horizonDays()).isEqualTo(7);
        assertThat(command.getValue().transactions()).singleElement().satisfies(transaction -> {
            assertThat(transaction.categoryKey()).isEqualTo("sales");
            assertThat(transaction.amount()).isEqualByComparingTo("12000");
            assertThat(transaction.currency()).isEqualTo(CLP);
            assertThat(transaction.date()).isEqualTo(START_DATE);
            assertThat(transaction.direction()).isEqualTo(TransactionDirection.CREDIT);
        });
    }

    @Test
    void returnsEmptyProjectionWhenNoProjectableMovementsExist() {
        var historyService = mock(CashflowMovementHistoryService.class);
        var projectionService = mock(CashflowProjectionService.class);
        var service = new CockpitProjectionService(historyService, projectionService);
        when(historyService.projectionReady(PROFILE_ID, START_DATE, START_DATE.plusDays(29))).thenReturn(List.of());

        var result = service.projectFromHistory(PROFILE_ID, BigDecimal.valueOf(250_000), START_DATE, 30);

        assertThat(result.dailyBalances()).isEmpty();
        assertThat(result.closingProjectedBalance()).isEqualByComparingTo("250000");
        assertThat(result.appliedObligations()).isEmpty();
        assertThat(result.alerts()).isEmpty();
        verify(projectionService, never()).project(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsHorizonBeyondMvpCapBeforeReadingHistory() {
        var historyService = mock(CashflowMovementHistoryService.class);
        var projectionService = mock(CashflowProjectionService.class);
        var service = new CockpitProjectionService(historyService, projectionService);

        assertThatThrownBy(() -> service.projectFromHistory(PROFILE_ID, BigDecimal.ZERO, START_DATE, 91))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horizonte no puede superar 90 días");

        verify(historyService, never()).projectionReady(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(projectionService, never()).project(org.mockito.ArgumentMatchers.any());
    }

    private static ProjectionReadyCashflowTransaction projectable(
            String categoryKey,
            long amount,
            LocalDate date,
            TransactionDirection direction
    ) {
        return new ProjectionReadyCashflowTransaction(
                UUID.randomUUID(),
                categoryKey,
                BigDecimal.valueOf(amount),
                CLP,
                date,
                direction,
                CashflowMovementStatus.PROJECTABLE
        );
    }
}
