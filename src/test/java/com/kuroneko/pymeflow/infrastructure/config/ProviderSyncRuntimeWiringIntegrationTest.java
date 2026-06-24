package com.kuroneko.pymeflow.infrastructure.config;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncStatusUseCase;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementEntry;
import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncPage;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncQuery;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProviderSyncRuntimeWiringIntegrationTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");
    private static final ProviderAuth AUTH = new ProviderAuth("fixture-provider", "credential-ref");
    private static final LocalDate DATE_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2026, 6, 30);

    private ApplicationServiceConfiguration configuration;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        var databaseName = "provider_sync_runtime_wiring_" + java.util.UUID.randomUUID().toString().replace("-", "");
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V5__create_provider_sync_sessions.sql"))
                .execute(dataSource);
        configuration = new ApplicationServiceConfiguration();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void wiredProviderSyncPersistsDurableStatusAcrossAdapterReinstantiation() {
        var sessionPort = configuration.syncSessionPort(jdbcTemplate);
        var useCase = configuration.providerSyncUseCase(
                queryRecorder(List.of(query -> new ProviderSyncPage(
                        List.of(entry("EXT-1")),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                ))),
                command -> new CashflowIngestionService.CashflowIngestionResult(
                        List.of(mock(CashflowIngestionService.CategorizedTransaction.class)),
                        List.of(),
                        List.of()
                ),
                sessionPort,
                new ProviderAuthConfig(3, 25)
        );

        var report = useCase.sync(command());
        var restartedStatus = new ProviderSyncStatusUseCase(configuration.syncSessionPort(jdbcTemplate));

        assertThat(restartedStatus.find(report.syncId())).hasValueSatisfying(snapshot -> {
            assertThat(snapshot.syncId()).isEqualTo(report.syncId());
            assertThat(snapshot.status()).isEqualTo(SyncSessionPort.SyncStatus.COMPLETED);
            assertThat(snapshot.entriesFetched()).isEqualTo(1);
            assertThat(snapshot.importedEntries()).isEqualTo(1);
            assertThat(snapshot.sessionEntryCount()).isEqualTo(1);
            assertThat(snapshot.durability()).isEqualTo(SyncSessionPort.Durability.DURABLE);
        });
    }

    @Test
    void wiredProviderSyncResumesFromDurableCursorForSameProfileAndProvider() {
        var firstRunQueries = new ArrayList<ProviderSyncQuery>();
        var firstSessionPort = configuration.syncSessionPort(jdbcTemplate);
        var firstUseCase = configuration.providerSyncUseCase(
                (query, auth) -> {
                    firstRunQueries.add(query);
                    return new ProviderSyncPage(List.of(entry("EXT-1")), Optional.of("cursor-2"), Optional.empty(), Optional.empty());
                },
                command -> new CashflowIngestionService.CashflowIngestionResult(
                        List.of(mock(CashflowIngestionService.CategorizedTransaction.class)),
                        List.of(),
                        List.of()
                ),
                firstSessionPort,
                new ProviderAuthConfig(1, 25)
        );
        firstUseCase.sync(command());

        var secondRunQueries = new ArrayList<ProviderSyncQuery>();
        var secondUseCase = configuration.providerSyncUseCase(
                (query, auth) -> {
                    secondRunQueries.add(query);
                    return new ProviderSyncPage(List.of(entry("EXT-2")), Optional.empty(), Optional.empty(), Optional.empty());
                },
                command -> new CashflowIngestionService.CashflowIngestionResult(
                        List.of(mock(CashflowIngestionService.CategorizedTransaction.class)),
                        List.of(),
                        List.of()
                ),
                configuration.syncSessionPort(jdbcTemplate),
                new ProviderAuthConfig(3, 25)
        );

        var secondReport = secondUseCase.sync(command());

        assertThat(firstRunQueries).hasSize(1);
        assertThat(firstRunQueries.getFirst().cursor()).isEmpty();
        assertThat(secondRunQueries).hasSize(1);
        assertThat(secondRunQueries.getFirst().cursor()).contains("cursor-2");
        assertThat(configuration.syncSessionPort(jdbcTemplate).findBySyncId(secondReport.syncId()))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.cursor()).isEmpty();
                    assertThat(snapshot.sessionEntryCount()).isEqualTo(2);
                    assertThat(snapshot.durability()).isEqualTo(SyncSessionPort.Durability.DURABLE);
                });
    }

    private static ProviderSyncPageProvider queryRecorder(List<ProviderSyncPageProvider> providers) {
        var remaining = new ArrayList<>(providers);
        return query -> remaining.removeFirst().fetch(query);
    }

    private static com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase.ProviderSyncCommand command() {
        return new com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase.ProviderSyncCommand(
                PROFILE_ID,
                DATE_FROM,
                DATE_TO,
                AUTH
        );
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

    @FunctionalInterface
    private interface ProviderSyncPageProvider extends com.kuroneko.pymeflow.application.port.out.BankProviderPort {
        @Override
        default ProviderSyncPage fetchStatements(ProviderSyncQuery query, ProviderAuth auth) {
            return fetch(query);
        }

        ProviderSyncPage fetch(ProviderSyncQuery query);
    }
}
