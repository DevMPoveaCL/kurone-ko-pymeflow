package com.kuroneko.pymeflow.infrastructure.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase;
import com.kuroneko.pymeflow.application.port.out.BankProviderPort;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementEntry;
import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncPage;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncQuery;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public final class FakeBankProviderAdapter implements BankProviderPort {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final String FIXTURE_PATH = "fixtures/provider/%s-%s.json";

    private final ClassLoader classLoader;
    private final ObjectMapper objectMapper;

    public FakeBankProviderAdapter() {
        this(Thread.currentThread().getContextClassLoader(), new ObjectMapper());
    }

    FakeBankProviderAdapter(ClassLoader classLoader, ObjectMapper objectMapper) {
        this.classLoader = classLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderSyncPage fetchStatements(ProviderSyncQuery query, ProviderAuth auth) {
        var pageKey = query.cursor().orElse("page-1");
        var fixturePath = FIXTURE_PATH.formatted(auth.providerType(), pageKey);
        try (var stream = classLoader.getResourceAsStream(fixturePath)) {
            if (stream == null) {
                return new ProviderSyncPage(List.of(), Optional.empty(), Optional.empty(), Optional.empty());
            }
            return toPage(objectMapper.readValue(stream, FixturePage.class));
        } catch (IOException exception) {
            throw new IllegalStateException("Provider fixture could not be loaded", exception);
        }
    }

    private static ProviderSyncPage toPage(FixturePage fixturePage) {
        var entries = fixturePage.entries().stream()
                .map(FakeBankProviderAdapter::toEntry)
                .toList();
        return new ProviderSyncPage(
                entries,
                Optional.ofNullable(fixturePage.nextCursor()).filter(value -> !value.isBlank()),
                Optional.ofNullable(fixturePage.totalPagesEstimate()),
                Optional.ofNullable(fixturePage.rateLimitResetsAt()).map(Instant::parse)
        );
    }

    private static ExternalStatementEntry toEntry(FixtureEntry fixtureEntry) {
        var currency = Currency.getInstance(fixtureEntry.currency());
        if (!CLP.equals(currency)) {
            throw new ProviderDataException(new ProviderError.DataError(
                    "currency",
                    "Only CLP provider statement rows are supported"
            ));
        }

        return new ExternalStatementEntry(
                fixtureEntry.externalReference(),
                LocalDate.parse(fixtureEntry.date()),
                fixtureEntry.description(),
                fixtureEntry.signedAmount().abs(),
                currency,
                directionFor(fixtureEntry.signedAmount()),
                fixtureEntry.counterpartyName(),
                fixtureEntry.accountAlias()
        );
    }

    private static TransactionDirection directionFor(BigDecimal signedAmount) {
        return signedAmount.signum() < 0 ? TransactionDirection.DEBIT : TransactionDirection.CREDIT;
    }

    private record FixturePage(
            String nextCursor,
            Integer totalPagesEstimate,
            String rateLimitResetsAt,
            List<FixtureEntry> entries
    ) {
        private FixturePage {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }

    private record FixtureEntry(
            String externalReference,
            String date,
            String description,
            BigDecimal signedAmount,
            String currency,
            String counterpartyName,
            String accountAlias
    ) {
    }

    public static final class ProviderDataException extends ProviderSyncUseCase.ProviderSyncException {

        private ProviderDataException(ProviderError.DataError providerError) {
            super(providerError);
        }

        public ProviderError.DataError providerError() {
            return (ProviderError.DataError) error();
        }
    }
}
