package com.kuroneko.pymeflow.infrastructure.bank;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementEntry;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportCommand;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulatedBankStatementAdapterTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final Currency USD = Currency.getInstance("USD");
    private static final ProfileId PROFILE_ID = new ProfileId("pharmacy-cl");
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 6, 19);

    private final CashflowIngestionService ingestionService = mock(CashflowIngestionService.class);
    private final SimulatedBankStatementAdapter adapter = new SimulatedBankStatementAdapter(ingestionService);

    @Test
    void mapsSignedNegativeToPositive() {
        importStatement(entry("BT-1", "Pago proveedor", new BigDecimal("-15000"), CLP));

        assertThat(capturedItem().transaction().amount()).isEqualByComparingTo("15000");
    }

    @Test
    void mapsPositiveAmountToPositiveAndKeepsBookingDate() {
        importStatement(entry("BT-2", "Venta caja", new BigDecimal("7500"), CLP));

        var transaction = capturedItem().transaction();
        assertThat(transaction.amount()).isEqualByComparingTo("7500");
        assertThat(transaction.bookedAt()).isEqualTo(BOOKING_DATE);
    }

    @Test
    void enrichesDescriptionWithoutLosingOriginalDescription() {
        importStatement(entry("BT-3", "Pago", new BigDecimal("15000"), CLP, "Farmacia", null));

        assertThat(capturedItem().transaction().description()).isEqualTo("Farmacia | Pago");
    }

    @Test
    void preservesDescriptionWithoutCounterparty() {
        importStatement(entry("BT-4", "Pago", new BigDecimal("15000"), CLP));

        assertThat(capturedItem().transaction().description()).isEqualTo("Pago");
    }

    @Test
    void rejectsNonClpCurrencyWithoutEchoingSubmittedCurrency() {
        var usdEntry = entry("BT-5", "Pago", new BigDecimal("15000"), USD);

        assertThatThrownBy(() -> adapter.importStatement(commandWith(List.of(usdEntry))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only CLP bank statement rows are supported")
                .hasMessageNotContaining("USD");
    }

    @Test
    void delegatesToIngestionServiceAndReturnsResultContract() {
        var expected = new CashflowIngestionService.CashflowIngestionResult(List.of(), List.of(), List.of());
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

        var result = adapter.importStatement(commandWith(List.of(entry(" BT-6 ", "Pago", new BigDecimal("15000"), CLP))));

        assertThat(result).isSameAs(expected);
        assertThat(capturedItem().externalReference()).isEqualTo("BT-6");
    }

    @Test
    void rejectsBlankExternalReferenceWithoutEchoingSubmittedValue() {
        assertThatThrownBy(() -> new ExternalStatementEntry("  ", BOOKING_DATE, "Pago", BigDecimal.TEN, CLP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External reference is required")
                .hasMessageNotContaining("  ");
    }

    @Test
    void dropsAccountAliasFromMappedIngestionItem() {
        importStatement(entry("BT-7", "Pago", new BigDecimal("15000"), CLP, null, "checking-account-1"));

        var item = capturedItem();
        assertThat(item.externalReference()).isEqualTo("BT-7");
        assertThat(item.transaction().description()).isEqualTo("Pago");
    }

    private CashflowIngestionService.CashflowIngestionCommand.IngestionItem capturedItem() {
        var captor = ArgumentCaptor.forClass(CashflowIngestionService.CashflowIngestionCommand.class);
        verify(ingestionService).ingest(captor.capture());
        return captor.getValue().items().getFirst();
    }

    private void importStatement(ExternalStatementEntry entry) {
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CashflowIngestionService.CashflowIngestionResult(List.of(), List.of(), List.of()));

        adapter.importStatement(commandWith(List.of(entry)));
    }

    private static ExternalStatementImportCommand commandWith(List<ExternalStatementEntry> entries) {
        return new ExternalStatementImportCommand(PROFILE_ID, "June statement", entries);
    }

    private static ExternalStatementEntry entry(String externalReference, String description, BigDecimal amount, Currency currency) {
        return new ExternalStatementEntry(externalReference, BOOKING_DATE, description, amount, currency);
    }

    private static ExternalStatementEntry entry(
            String externalReference,
            String description,
            BigDecimal amount,
            Currency currency,
            String counterpartyName,
            String accountAlias
    ) {
        return new ExternalStatementEntry(
                externalReference,
                BOOKING_DATE,
                description,
                amount,
                currency,
                counterpartyName,
                accountAlias
        );
    }
}
