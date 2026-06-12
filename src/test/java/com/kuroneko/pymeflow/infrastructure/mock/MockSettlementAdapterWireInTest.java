package com.kuroneko.pymeflow.infrastructure.mock;

import com.kuroneko.pymeflow.application.port.out.SettlementFeedPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockSettlementAdapterWireInTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MockBankSettlementAdapter.class, MockAcquirerSettlementAdapter.class);

    @Test
    void wiresMockSettlementAdaptersOnlyWhenMockProfilesAreActive() {
        contextRunner
                .withPropertyValues("spring.profiles.active=mock-bank,mock-acquirer")
                .run(context -> {
                    Map<String, SettlementFeedPort> settlementFeedPorts = context.getBeansOfType(SettlementFeedPort.class);

                    assertThat(settlementFeedPorts.values())
                            .hasSize(2)
                            .extracting(Object::getClass)
                            .containsExactlyInAnyOrder(MockBankSettlementAdapter.class, MockAcquirerSettlementAdapter.class);
                });
    }

    @Test
    void keepsMockSettlementAdaptersDisabledWithoutMockProfiles() {
        contextRunner.run(context ->
                assertThat(context.getBeansOfType(SettlementFeedPort.class)).isEmpty()
        );
    }
}
