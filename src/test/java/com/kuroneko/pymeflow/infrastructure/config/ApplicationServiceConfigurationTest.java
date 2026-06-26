package com.kuroneko.pymeflow.infrastructure.config;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementHistoryService;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionService;
import com.kuroneko.pymeflow.application.cashflow.CockpitProjectionService;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncStatusUseCase;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase;
import com.kuroneko.pymeflow.application.port.out.BankProviderPort;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapter;
import com.kuroneko.pymeflow.infrastructure.persistence.JdbcSyncSessionAdapter;
import com.kuroneko.pymeflow.infrastructure.provider.FakeBankProviderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApplicationServiceConfigurationTest {

    @Test
    void wiresSimulatedBankStatementAdapterBehindExternalStatementImportPort() {
        var configuration = new ApplicationServiceConfiguration();

        ExternalStatementImportPort port = configuration.externalStatementImportPort(mock(CashflowIngestionService.class));

        assertThat(port).isInstanceOf(SimulatedBankStatementAdapter.class);
    }

    @Test
    void wiresProviderSyncServiceWithJdbcSessionAdapter() {
        var configuration = new ApplicationServiceConfiguration();

        SyncSessionPort sessionPort = configuration.syncSessionPort(mock(JdbcTemplate.class));
        ProviderSyncUseCase useCase = configuration.providerSyncUseCase(
                mock(BankProviderPort.class),
                mock(ExternalStatementImportPort.class),
                sessionPort,
                new ProviderAuthConfig(3, 25)
        );

        assertThat(sessionPort).isInstanceOf(JdbcSyncSessionAdapter.class);
        assertThat(useCase).isInstanceOf(ProviderSyncUseCase.ProviderSyncService.class);
    }

    @Test
    void wiresProviderSyncStatusUseCaseWithSessionPort() {
        var configuration = new ApplicationServiceConfiguration();

        ProviderSyncStatusUseCase statusUseCase = configuration.providerSyncStatusUseCase(mock(SyncSessionPort.class));

        assertThat(statusUseCase).isInstanceOf(ProviderSyncStatusUseCase.class);
    }

    @Test
    void wiresCockpitProjectionServiceWithHistoryAndProjectionServices() {
        var configuration = new ApplicationServiceConfiguration();

        var service = configuration.cockpitProjectionService(
                mock(CashflowMovementHistoryService.class),
                mock(CashflowProjectionService.class)
        );

        assertThat(service).isInstanceOf(CockpitProjectionService.class);
    }

    @Test
    void wiresFakeProviderAdapterBehindProviderPort() {
        var configuration = new ApplicationServiceConfiguration();

        BankProviderPort providerPort = configuration.bankProviderPort();

        assertThat(providerPort).isInstanceOf(FakeBankProviderAdapter.class);
    }
}
