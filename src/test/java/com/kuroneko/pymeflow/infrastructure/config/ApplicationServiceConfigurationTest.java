package com.kuroneko.pymeflow.infrastructure.config;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase;
import com.kuroneko.pymeflow.application.port.out.BankProviderPort;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapter;
import com.kuroneko.pymeflow.infrastructure.provider.FakeBankProviderAdapter;
import com.kuroneko.pymeflow.infrastructure.provider.InMemorySyncSessionAdapter;
import org.junit.jupiter.api.Test;

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
    void wiresProviderSyncServiceWithInMemorySessionAdapter() {
        var configuration = new ApplicationServiceConfiguration();

        SyncSessionPort sessionPort = configuration.syncSessionPort();
        ProviderSyncUseCase useCase = configuration.providerSyncUseCase(
                mock(BankProviderPort.class),
                mock(ExternalStatementImportPort.class),
                sessionPort,
                new ProviderAuthConfig(3, 25)
        );

        assertThat(sessionPort).isInstanceOf(InMemorySyncSessionAdapter.class);
        assertThat(useCase).isInstanceOf(ProviderSyncUseCase.ProviderSyncService.class);
    }

    @Test
    void wiresFakeProviderAdapterBehindProviderPort() {
        var configuration = new ApplicationServiceConfiguration();

        BankProviderPort providerPort = configuration.bankProviderPort();

        assertThat(providerPort).isInstanceOf(FakeBankProviderAdapter.class);
    }
}
