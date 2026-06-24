package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.BankProviderPort;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementEntry;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportCommand;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncPage;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncQuery;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderSyncUseCaseTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 6, 30);
    private static final ProviderAuth AUTH = new ProviderAuth("fixture-provider", "credential-ref");

    private final BankProviderPort bankProviderPort = mock(BankProviderPort.class);
    private final ExternalStatementImportPort importPort = mock(ExternalStatementImportPort.class);
    private final SyncSessionPort syncSessionPort = mock(SyncSessionPort.class);
    private final ProviderSyncUseCase useCase = new ProviderSyncUseCase.ProviderSyncService(
            bankProviderPort,
            importPort,
            syncSessionPort,
            2,
            25
    );

    @Test
    void singlePageSyncImportsAllEntriesAndCompletesWithoutMorePages() {
        var firstEntry = entry("EXT-1");
        var secondEntry = entry("EXT-2");
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenReturn(page(List.of(firstEntry, secondEntry), Optional.empty()));
        when(importPort.importStatement(any())).thenReturn(ingestionResult(1, 1, 0));

        var report = useCase.sync(command());

        assertThat(report.pagesFetched()).isEqualTo(1);
        assertThat(report.entriesFetched()).isEqualTo(2);
        assertThat(report.importedEntries()).isEqualTo(2);
        assertThat(report.hasMorePages()).isFalse();
        assertThat(report.errors()).isEmpty();
        assertThat(report.truncated()).isFalse();
        assertThat(capturedImport().entries()).containsExactly(firstEntry, secondEntry);
    }

    @Test
    void multiPageSyncFollowsCursorChainAndImportsEveryPage() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenReturn(page(List.of(entry("EXT-1")), Optional.of("cursor-2")))
                .thenReturn(page(List.of(entry("EXT-2")), Optional.empty()));
        when(importPort.importStatement(any())).thenReturn(ingestionResult(1, 0, 0));

        var report = useCase.sync(command());

        assertThat(report.pagesFetched()).isEqualTo(2);
        assertThat(report.entriesFetched()).isEqualTo(2);
        assertThat(report.importedEntries()).isEqualTo(2);
        assertThat(report.hasMorePages()).isFalse();
        var queries = capturedQueries();
        assertThat(queries.get(0).cursor()).isEmpty();
        assertThat(queries.get(1).cursor()).contains("cursor-2");
    }

    @Test
    void maxPageGuardStopsAtConfiguredLimitAndMarksReportTruncated() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenReturn(page(List.of(entry("EXT-1")), Optional.of("cursor-2")))
                .thenReturn(page(List.of(entry("EXT-2")), Optional.of("cursor-3")));
        when(importPort.importStatement(any())).thenReturn(ingestionResult(1, 0, 0));

        var report = useCase.sync(command());

        assertThat(report.pagesFetched()).isEqualTo(2);
        assertThat(report.truncated()).isTrue();
        assertThat(report.hasMorePages()).isTrue();
        verify(bankProviderPort, times(2)).fetchStatements(any(), any());
    }

    @Test
    void authErrorAbortsSyncAndRecordsErrorWithoutImportingPage() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        var error = new ProviderError.AuthError("credential rejected");
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(error));

        var report = useCase.sync(command());

        assertThat(report.errors()).containsExactly(error);
        assertThat(report.authAborted()).isTrue();
        assertThat(report.pagesFetched()).isZero();
        verify(importPort, never()).importStatement(any());
    }

    @Test
    void rateLimitErrorSurfacesRetryAfterSecondsInReport() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        var error = new ProviderError.RateLimitError(90, "too many requests");
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(error));

        var report = useCase.sync(command());

        assertThat(report.errors()).containsExactly(error);
        assertThat(report.retryAfterSeconds()).contains(90);
        assertThat(report.authAborted()).isFalse();
    }

    @Test
    void unavailableAndDataErrorsAreAggregatedWithoutAuthAbort() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        var unavailable = new ProviderError.UnavailableError("temporary outage");
        var data = new ProviderError.DataError("currency", "Only CLP provider statement rows are supported");
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(unavailable))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(data));

        var report = useCase.sync(command());

        assertThat(report.errors()).containsExactly(unavailable, data);
        assertThat(report.authAborted()).isFalse();
        assertThat(((ProviderError.DataError) report.errors().get(1)).field()).isEqualTo("currency");
        assertThat(((ProviderError.DataError) report.errors().get(1)).detail())
                .isEqualTo("Only CLP provider statement rows are supported");
    }

    @Test
    void unavailableErrorIsCollectedAndSyncContinuesToNextRecoverablePage() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        var unavailable = new ProviderError.UnavailableError("temporary outage");
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(unavailable))
                .thenReturn(page(List.of(entry("EXT-RECOVERED")), Optional.empty()));
        when(importPort.importStatement(any())).thenReturn(ingestionResult(1, 0, 0));

        var report = useCase.sync(command());

        assertThat(report.errors()).containsExactly(unavailable);
        assertThat(report.authAborted()).isFalse();
        assertThat(report.pagesFetched()).isEqualTo(1);
        assertThat(report.entriesFetched()).isEqualTo(1);
        assertThat(report.importedEntries()).isEqualTo(1);
        assertThat(capturedImport().entries())
                .extracting(ExternalStatementEntry::externalReference)
                .containsExactly("EXT-RECOVERED");
    }

    @Test
    void repeatedUnavailableErrorsStopAtMaxAttemptsWithoutImporting() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.empty());
        var firstOutage = new ProviderError.UnavailableError("first outage");
        var secondOutage = new ProviderError.UnavailableError("second outage");
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(firstOutage))
                .thenThrow(new ProviderSyncUseCase.ProviderSyncException(secondOutage));

        var report = useCase.sync(command());

        assertThat(report.errors()).containsExactly(firstOutage, secondOutage);
        assertThat(report.authAborted()).isFalse();
        assertThat(report.pagesFetched()).isZero();
        assertThat(report.truncated()).isTrue();
        verify(importPort, never()).importStatement(any());
        verify(bankProviderPort, times(2)).fetchStatements(any(), any());
    }

    @Test
    void sessionCursorIsScopedByProfileAndProviderAndReportExposesSyncId() {
        when(syncSessionPort.syncId(PROFILE_ID, AUTH.providerType())).thenReturn("sync-fixture-provider-001");
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.of("provider-cursor"));
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenReturn(page(List.of(entry("EXT-1")), Optional.empty()));
        when(importPort.importStatement(any())).thenReturn(ingestionResult(1, 0, 0));

        var report = useCase.sync(command());

        assertThat(report.syncId()).isEqualTo("sync-fixture-provider-001");
        assertThat(capturedQueriesForSinglePage().getFirst().cursor()).contains("provider-cursor");
        verify(syncSessionPort).saveCursor(PROFILE_ID, AUTH.providerType(), "");
        verify(syncSessionPort).incrementEntryCount(PROFILE_ID, AUTH.providerType(), 1);
    }

    @Test
    void sessionCursorIsUsedForResumeAndSavedAfterEachPage() {
        when(syncSessionPort.findCursor(PROFILE_ID, AUTH.providerType())).thenReturn(Optional.of("cursor-resume"));
        when(bankProviderPort.fetchStatements(any(), any()))
                .thenReturn(page(List.of(entry("EXT-1")), Optional.of("cursor-next")))
                .thenReturn(page(List.of(entry("EXT-2")), Optional.empty()));
        when(importPort.importStatement(any())).thenReturn(ingestionResult(1, 0, 0));

        var report = useCase.sync(command());

        assertThat(report.pagesFetched()).isEqualTo(2);
        assertThat(capturedQueries().getFirst().cursor()).contains("cursor-resume");
        verify(syncSessionPort).saveCursor(PROFILE_ID, AUTH.providerType(), "cursor-next");
        verify(syncSessionPort).saveCursor(PROFILE_ID, AUTH.providerType(), "");
        verify(syncSessionPort, times(2)).incrementEntryCount(PROFILE_ID, AUTH.providerType(), 1);
    }

    private ExternalStatementImportCommand capturedImport() {
        var captor = ArgumentCaptor.forClass(ExternalStatementImportCommand.class);
        verify(importPort).importStatement(captor.capture());
        return captor.getValue();
    }

    private List<ProviderSyncQuery> capturedQueries() {
        var captor = ArgumentCaptor.forClass(ProviderSyncQuery.class);
        verify(bankProviderPort, times(2)).fetchStatements(captor.capture(), any());
        return captor.getAllValues();
    }

    private List<ProviderSyncQuery> capturedQueriesForSinglePage() {
        var captor = ArgumentCaptor.forClass(ProviderSyncQuery.class);
        verify(bankProviderPort).fetchStatements(captor.capture(), any());
        return captor.getAllValues();
    }

    private static ProviderSyncUseCase.ProviderSyncCommand command() {
        return new ProviderSyncUseCase.ProviderSyncCommand(PROFILE_ID, DATE_FROM, DATE_TO, AUTH);
    }

    private static ProviderSyncPage page(List<ExternalStatementEntry> entries, Optional<String> nextCursor) {
        return new ProviderSyncPage(entries, nextCursor, Optional.empty(), Optional.empty());
    }

    private static ExternalStatementEntry entry(String externalReference) {
        return new ExternalStatementEntry(
                externalReference,
                LocalDate.of(2026, 6, 20),
                "Card payment",
                new BigDecimal("15000"),
                Currency.getInstance("CLP")
        );
    }

    private static CashflowIngestionService.CashflowIngestionResult ingestionResult(
            int categorized,
            int manualReview,
            int rejected
    ) {
        return new CashflowIngestionService.CashflowIngestionResult(
                java.util.Collections.nCopies(categorized, mock(CashflowIngestionService.CategorizedTransaction.class)),
                java.util.Collections.nCopies(manualReview, mock(CashflowIngestionService.ManualReviewTransaction.class)),
                java.util.Collections.nCopies(rejected, mock(CashflowIngestionService.RejectedTransaction.class))
        );
    }
}
