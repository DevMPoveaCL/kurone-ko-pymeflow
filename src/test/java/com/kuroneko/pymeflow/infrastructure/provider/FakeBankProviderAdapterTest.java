package com.kuroneko.pymeflow.infrastructure.provider;

import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncQuery;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeBankProviderAdapterTest {
    private static final ProfileId PROFILE_ID = new ProfileId("pharmacy-cl");
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 6, 30);

    private final FakeBankProviderAdapter adapter = new FakeBankProviderAdapter();

    @Test
    void loadsSantanderFixtureAndMapsSignedAmountsToDirectionAndPositiveAmounts() {
        var page = adapter.fetchStatements(query(Optional.empty()), auth("santander"));

        assertThat(page.entries())
                .extracting(entry -> entry.externalReference() + ":" + entry.direction() + ":" + entry.amount())
                .containsExactly("SAN-001:DEBIT:15000.00", "SAN-002:CREDIT:87500.00");
        assertThat(page.nextCursor()).contains("page-2");
        assertThat(page.totalPagesEstimate()).contains(2);
        assertThat(page.rateLimitResetsAt()).contains(Instant.parse("2026-06-20T10:15:30Z"));
    }

    @Test
    void loadsBancoEstadoFixtureWithCounterpartyMetadata() {
        var page = adapter.fetchStatements(query(Optional.empty()), auth("bancoestado"));

        assertThat(page.entries())
                .extracting(entry -> entry.externalReference() + ":" + entry.counterpartyName() + ":" + entry.accountAlias())
                .containsExactly("BE-001:Proveedor Uno:checking-main", "BE-002:Cliente Caja:checking-main");
    }

    @Test
    void missingFixtureReturnsEmptyPage() {
        var page = adapter.fetchStatements(query(Optional.of("page-99")), auth("santander"));

        assertThat(page.entries()).isEmpty();
        assertThat(page.nextCursor()).isEmpty();
        assertThat(page.totalPagesEstimate()).isEmpty();
        assertThat(page.rateLimitResetsAt()).isEmpty();
    }

    @Test
    void nonClpFixtureThrowsSafeProviderDataError() {
        assertThatThrownBy(() -> adapter.fetchStatements(query(Optional.empty()), auth("unsupported-currency")))
                .isInstanceOf(FakeBankProviderAdapter.ProviderDataException.class)
                .satisfies(error -> assertThat(((FakeBankProviderAdapter.ProviderDataException) error).providerError())
                        .isEqualTo(new ProviderError.DataError("currency", "Only CLP provider statement rows are supported")))
                .hasMessage("Only CLP provider statement rows are supported")
                .hasMessageNotContaining("USD");
    }

    private static ProviderSyncQuery query(Optional<String> cursor) {
        return new ProviderSyncQuery(PROFILE_ID, DATE_FROM, DATE_TO, cursor, 100);
    }

    private static ProviderAuth auth(String providerType) {
        return new ProviderAuth(providerType, "fixture-ref");
    }
}
