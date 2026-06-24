package com.kuroneko.pymeflow.infrastructure.config;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementHistoryService;
import com.kuroneko.pymeflow.application.cashflow.CashflowProjectionService;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewResolutionService;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncStatusUseCase;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase;
import com.kuroneko.pymeflow.application.cashflow.SensitiveDataPolicy;
import com.kuroneko.pymeflow.application.export.AccountantExportService;
import com.kuroneko.pymeflow.application.port.out.AccountantExportPort;
import com.kuroneko.pymeflow.application.port.out.BankProviderPort;
import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.application.port.out.ProfileRegistryPort;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.application.recommendation.HistoryRecommendationService;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.infrastructure.bank.SimulatedBankStatementAdapter;
import com.kuroneko.pymeflow.infrastructure.persistence.JdbcSyncSessionAdapter;
import com.kuroneko.pymeflow.infrastructure.provider.FakeBankProviderAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ApplicationServiceConfiguration {

    @Bean
    @ConditionalOnBean(AccountantExportPort.class)
    AccountantExportService accountantExportService(AccountantExportPort accountantExportPort) {
        return new AccountantExportService(accountantExportPort);
    }

    @Bean
    VerticalProfileService verticalProfileService(ProfileRegistryPort profileRegistryPort) {
        return new VerticalProfileService(profileRegistryPort);
    }

    @Bean
    CashflowIngestionService cashflowIngestionService(
            VerticalProfileService verticalProfileService,
            CashflowCategorizationPort cashflowCategorizationPort,
            SensitiveDataPolicy sensitiveDataPolicy,
            CashflowMovementHistoryPort cashflowMovementHistoryPort
    ) {
        return new CashflowIngestionService(
                verticalProfileService,
                cashflowCategorizationPort,
                sensitiveDataPolicy,
                cashflowMovementHistoryPort
        );
    }

    @Bean
    CashflowProjectionService cashflowProjectionService(VerticalProfileService verticalProfileService) {
        return new CashflowProjectionService(verticalProfileService);
    }

    @Bean
    CashflowMovementHistoryService cashflowMovementHistoryService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort cashflowMovementHistoryPort,
            SensitiveDataPolicy sensitiveDataPolicy
    ) {
        return new CashflowMovementHistoryService(verticalProfileService, cashflowMovementHistoryPort, sensitiveDataPolicy);
    }

    @Bean
    HistoryRecommendationService historyRecommendationService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort cashflowMovementHistoryPort
    ) {
        return new HistoryRecommendationService(verticalProfileService, cashflowMovementHistoryPort);
    }

    @Bean
    ManualReviewResolutionService manualReviewResolutionService(
            VerticalProfileService verticalProfileService,
            SensitiveDataPolicy sensitiveDataPolicy
    ) {
        return new ManualReviewResolutionService(verticalProfileService, sensitiveDataPolicy);
    }

    @Bean
    SensitiveDataPolicy sensitiveDataPolicy(VerticalProfileProperties properties) {
        return new SensitiveDataPolicy(properties.sensitiveIdentifiers());
    }

    @Bean
    ExternalStatementImportPort externalStatementImportPort(CashflowIngestionService cashflowIngestionService) {
        return new SimulatedBankStatementAdapter(cashflowIngestionService);
    }

    @Bean
    BankProviderPort bankProviderPort() {
        return new FakeBankProviderAdapter();
    }

    @Bean
    SyncSessionPort syncSessionPort(JdbcTemplate jdbcTemplate) {
        return new JdbcSyncSessionAdapter(jdbcTemplate);
    }

    @Bean
    ProviderSyncUseCase providerSyncUseCase(
            BankProviderPort bankProviderPort,
            ExternalStatementImportPort externalStatementImportPort,
            SyncSessionPort syncSessionPort,
            ProviderAuthConfig providerAuthConfig
    ) {
        return new ProviderSyncUseCase.ProviderSyncService(
                bankProviderPort,
                externalStatementImportPort,
                syncSessionPort,
                providerAuthConfig.maxPages(),
                providerAuthConfig.pageSize()
        );
    }

    @Bean
    ProviderSyncStatusUseCase providerSyncStatusUseCase(SyncSessionPort syncSessionPort) {
        return new ProviderSyncStatusUseCase(syncSessionPort);
    }
}
