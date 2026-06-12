package com.kuroneko.pymeflow.infrastructure.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kuroneko.pymeflow.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockSettlementAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void bankAdapterReturnsSimulationOnlySettlementFeed() {
        var adapter = new MockBankSettlementAdapter();

        var feed = adapter.fetchSettlements(new TenantId(UUID.randomUUID()), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(feed.entries()).hasSize(2);
        assertThat(feed.entries().getFirst().source()).isEqualTo("simulation-bank");
    }

    @Test
    void acquirerAdapterReturnsSimulationOnlySettlementFeed() {
        var adapter = new MockAcquirerSettlementAdapter();

        var feed = adapter.fetchSettlements(new TenantId(UUID.randomUUID()), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(feed.entries()).hasSize(2);
        assertThat(feed.entries().getFirst().source()).isEqualTo("simulation-acquirer");
    }

    @Test
    void settlementFeedJsonMatchesContractShape() throws Exception {
        var adapter = new MockAcquirerSettlementAdapter();
        var feed = adapter.fetchSettlements(new TenantId(UUID.randomUUID()), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        var json = objectMapper.readTree(objectMapper.writeValueAsString(feed));

        assertThat(json.get("tenantId").get("value").isTextual()).isTrue();
        assertThat(json.get("entries").isArray()).isTrue();
        assertThat(json.get("entries")).hasSize(2);
        var entry = json.get("entries").get(0);
        assertThat(entry.get("source").asText()).isEqualTo("simulation-acquirer");
        assertThat(entry.get("settledAt").asText()).isEqualTo("2026-06-01");
        assertThat(entry.get("grossAmount").isNumber()).isTrue();
        assertThat(entry.get("feeAmount").isNumber()).isTrue();
        assertThat(entry.get("currency").asText()).isEqualTo("CLP");
    }
}
