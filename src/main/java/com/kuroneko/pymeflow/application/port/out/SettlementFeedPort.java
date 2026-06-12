package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.tenant.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

public interface SettlementFeedPort {
    SettlementFeed fetchSettlements(TenantId tenantId, LocalDate from, LocalDate to);

    record SettlementFeed(TenantId tenantId, List<SettlementEntry> entries) {
        public SettlementFeed {
            if (tenantId == null) {
                throw new IllegalArgumentException("Tenant id is required");
            }
            entries = List.copyOf(entries == null ? List.of() : entries);
        }
    }

    record SettlementEntry(String source, LocalDate settledAt, BigDecimal grossAmount, BigDecimal feeAmount, Currency currency) {
        public SettlementEntry {
            requireText(source, "Source is required");
            if (settledAt == null) {
                throw new IllegalArgumentException("Settlement date is required");
            }
            if (grossAmount == null) {
                throw new IllegalArgumentException("Gross amount is required");
            }
            if (feeAmount == null) {
                throw new IllegalArgumentException("Fee amount is required");
            }
            if (currency == null) {
                throw new IllegalArgumentException("Currency is required");
            }
        }

        private static void requireText(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
