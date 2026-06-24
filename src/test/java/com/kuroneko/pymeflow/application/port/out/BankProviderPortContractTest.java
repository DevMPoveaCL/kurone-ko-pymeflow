package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class AbstractBankProviderPortContractTest {

    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 6, 30);

    protected abstract BankProviderPort providerPort();

    @Test
    void providerPortFetchesStatementsFromQueryAndAuth() {
        var entry = entry("EXT-1");

        var page = providerPort().fetchStatements(
                new ProviderSyncQuery(PROFILE_ID, DATE_FROM, DATE_TO, Optional.empty(), 100),
                new ProviderAuth("fixture-provider", "credential-ref")
        );

        assertThat(page.entries()).containsExactly(entry);
        assertThat(page.nextCursor()).contains("cursor-2");
        assertThat(page.totalPagesEstimate()).contains(2);
        assertThat(page.rateLimitResetsAt()).contains(Instant.parse("2026-06-20T10:15:30Z"));
    }

    @Test
    void queryRejectsMissingRequiredFieldsInvalidWindowAndInvalidPageSize() {
        assertThatThrownBy(() -> new ProviderSyncQuery(null, DATE_FROM, DATE_TO, Optional.empty(), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile id is required");
        assertThatThrownBy(() -> new ProviderSyncQuery(PROFILE_ID, null, DATE_TO, Optional.empty(), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date from is required");
        assertThatThrownBy(() -> new ProviderSyncQuery(PROFILE_ID, DATE_FROM, null, Optional.empty(), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date to is required");
        assertThatThrownBy(() -> new ProviderSyncQuery(PROFILE_ID, DATE_TO, DATE_FROM, Optional.empty(), 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date from must be on or before date to");
        assertThatThrownBy(() -> new ProviderSyncQuery(PROFILE_ID, DATE_FROM, DATE_TO, Optional.empty(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than zero");
    }

    @Test
    void pageDefensivelyCopiesEntriesAndKeepsLastPageCursorEmpty() {
        var entries = new ArrayList<>(List.of(entry("EXT-1")));

        var page = new ProviderSyncPage(entries, Optional.empty(), Optional.empty(), Optional.empty());
        entries.add(entry("EXT-2"));

        assertThat(page.entries())
                .extracting(ExternalStatementEntry::externalReference)
                .containsExactly("EXT-1");
        assertThatThrownBy(() -> page.entries().add(entry("EXT-3")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(page.nextCursor()).isEmpty();
    }

    @Test
    void authRejectsMissingSafeDescriptors() {
        var auth = new ProviderAuth("fixture-provider", "credential-ref");

        assertThat(auth.providerType()).isEqualTo("fixture-provider");
        assertThat(auth.credentialRef()).isEqualTo("credential-ref");
        assertThatThrownBy(() -> new ProviderAuth(null, "credential-ref"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provider type is required");
        assertThatThrownBy(() -> new ProviderAuth("fixture-provider", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credential reference is required");
    }

    @Test
    void providerErrorsAreSealedExhaustiveAndCarrySafeFields() {
        assertThat(describe(new ProviderError.AuthError("credential rejected")))
                .isEqualTo("auth:credential rejected");
        assertThat(describe(new ProviderError.RateLimitError(90, "too many requests")))
                .isEqualTo("rate-limit:90:too many requests");
        assertThat(describe(new ProviderError.UnavailableError("temporary outage")))
                .isEqualTo("unavailable:temporary outage");
        assertThat(describe(new ProviderError.DataError("currency", "Unsupported currency")))
                .isEqualTo("data:currency:Unsupported currency");
    }

    private static String describe(ProviderError error) {
        return switch (error) {
            case ProviderError.AuthError auth -> "auth:" + auth.safeMessage();
            case ProviderError.RateLimitError rateLimit -> "rate-limit:"
                    + rateLimit.retryAfterSeconds() + ":" + rateLimit.safeMessage();
            case ProviderError.UnavailableError unavailable -> "unavailable:" + unavailable.safeMessage();
            case ProviderError.DataError data -> "data:" + data.field() + ":" + data.detail();
        };
    }

    protected static ExternalStatementEntry entry(String externalReference) {
        return new ExternalStatementEntry(
                externalReference,
                LocalDate.of(2026, 6, 19),
                "Card payment",
                new BigDecimal("15000.00"),
                Currency.getInstance("CLP")
        );
    }
}

class BankProviderPortContractTest extends AbstractBankProviderPortContractTest {
    @Override
    protected BankProviderPort providerPort() {
        var entry = entry("EXT-1");
        return (query, auth) -> new ProviderSyncPage(
                List.of(entry),
                Optional.of("cursor-2"),
                Optional.of(2),
                Optional.of(Instant.parse("2026-06-20T10:15:30Z"))
        );
    }
}
