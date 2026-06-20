package com.kuroneko.pymeflow.infrastructure.config;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapter;
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
}
