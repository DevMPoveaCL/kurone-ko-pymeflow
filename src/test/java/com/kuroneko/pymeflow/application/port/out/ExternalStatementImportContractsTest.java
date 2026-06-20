package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalStatementImportContractsTest {

    @Test
    void externalStatementImportPortReturnsCashflowIngestionResult() {
        var expected = new CashflowIngestionService.CashflowIngestionResult(List.of(), List.of(), List.of());
        ExternalStatementImportPort port = command -> expected;
        var command = commandWith(List.of(entry("EXT-1")));

        var result = port.importStatement(command);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void commandRequiresProfileIdAndDefensivelyCopiesEntries() {
        var entries = new java.util.ArrayList<>(List.of(entry("EXT-1")));

        var command = new ExternalStatementImportCommand(new ProfileId("retail-cl"), "June import", entries);
        entries.add(entry("EXT-2"));

        assertThat(command.profileId()).isEqualTo(new ProfileId("retail-cl"));
        assertThat(command.importLabel()).isEqualTo("June import");
        assertThat(command.entries())
                .extracting(ExternalStatementEntry::externalReference)
                .containsExactly("EXT-1");
        assertThatThrownBy(() -> command.entries().add(entry("EXT-3")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void commandRejectsMissingProfileId() {
        assertThatThrownBy(() -> new ExternalStatementImportCommand(null, null, List.of(entry("EXT-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile id is required");
    }

    @Test
    void entryAcceptsValidBankAgnosticStatementFields() {
        var date = LocalDate.of(2026, 6, 19);
        var amount = new BigDecimal("15000.00");
        var currency = Currency.getInstance("CLP");

        var entry = new ExternalStatementEntry("EXT-1", date, "Card payment", amount, currency);

        assertThat(entry.externalReference()).isEqualTo("EXT-1");
        assertThat(entry.date()).isEqualTo(date);
        assertThat(entry.description()).isEqualTo("Card payment");
        assertThat(entry.amount()).isEqualByComparingTo("15000.00");
        assertThat(entry.currency()).isEqualTo(currency);
    }

    @Test
    void entryRejectsMissingBlankAndZeroFields() {
        var date = LocalDate.of(2026, 6, 19);
        var amount = new BigDecimal("15000.00");
        var currency = Currency.getInstance("CLP");

        assertThatThrownBy(() -> new ExternalStatementEntry(" ", date, "Card payment", amount, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External reference is required");
        assertThatThrownBy(() -> new ExternalStatementEntry("EXT-1", null, "Card payment", amount, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date is required");
        assertThatThrownBy(() -> new ExternalStatementEntry("EXT-1", date, " ", amount, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description is required");
        assertThatThrownBy(() -> new ExternalStatementEntry("EXT-1", date, "Card payment", BigDecimal.ZERO, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be non-zero");
        assertThatThrownBy(() -> new ExternalStatementEntry("EXT-1", date, "Card payment", amount, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency is required");
    }

    private static ExternalStatementImportCommand commandWith(List<ExternalStatementEntry> entries) {
        return new ExternalStatementImportCommand(new ProfileId("retail-cl"), null, entries);
    }

    private static ExternalStatementEntry entry(String externalReference) {
        return new ExternalStatementEntry(
                externalReference,
                LocalDate.of(2026, 6, 19),
                "Card payment",
                new BigDecimal("15000.00"),
                Currency.getInstance("CLP")
        );
    }
}
