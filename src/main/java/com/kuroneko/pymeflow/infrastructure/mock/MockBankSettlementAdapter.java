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
@Profile("mock-bank")
public class MockBankSettlementAdapter implements SettlementFeedPort {
    @Override
    public SettlementFeed fetchSettlements(TenantId tenantId, LocalDate from, LocalDate to) {
        var currency = Currency.getInstance("CLP");
        var settlementDate = from == null ? LocalDate.now() : from;
        return new SettlementFeed(tenantId, List.of(
                new SettlementEntry("simulation-bank", settlementDate, BigDecimal.valueOf(185000), BigDecimal.valueOf(900), currency),
                new SettlementEntry("simulation-bank", settlementDate.plusDays(1), BigDecimal.valueOf(132000), BigDecimal.valueOf(650), currency)
        ));
    }
}
