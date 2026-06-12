package com.kuroneko.pymeflow.infrastructure.mock;

import com.kuroneko.pymeflow.application.port.out.SettlementFeedPort;
import com.kuroneko.pymeflow.domain.tenant.TenantId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@Component
@Profile("mock-acquirer")
public class MockAcquirerSettlementAdapter implements SettlementFeedPort {
    @Override
    public SettlementFeed fetchSettlements(TenantId tenantId, LocalDate from, LocalDate to) {
        var currency = Currency.getInstance("CLP");
        var settlementDate = from == null ? LocalDate.now() : from;
        return new SettlementFeed(tenantId, List.of(
                new SettlementEntry("simulation-acquirer", settlementDate, BigDecimal.valueOf(420000), BigDecimal.valueOf(6300), currency),
                new SettlementEntry("simulation-acquirer", settlementDate.plusDays(1), BigDecimal.valueOf(360000), BigDecimal.valueOf(5400), currency)
        ));
    }
}
