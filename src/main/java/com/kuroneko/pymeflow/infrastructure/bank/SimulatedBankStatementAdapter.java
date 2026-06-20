package com.kuroneko.pymeflow.infrastructure.bank;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportCommand;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;

import java.util.Currency;

public final class SimulatedBankStatementAdapter implements ExternalStatementImportPort {
    private static final Currency CLP = Currency.getInstance("CLP");

    private final CashflowIngestionService cashflowIngestionService;

    public SimulatedBankStatementAdapter(CashflowIngestionService cashflowIngestionService) {
        this.cashflowIngestionService = cashflowIngestionService;
    }

    @Override
    public CashflowIngestionService.CashflowIngestionResult importStatement(ExternalStatementImportCommand command) {
        var items = command.entries().stream()
                .map(entry -> {
                    if (!CLP.equals(entry.currency())) {
                        throw new IllegalArgumentException("Only CLP bank statement rows are supported");
                    }
                    var transaction = new Transaction(
                            descriptionFor(entry.counterpartyName(), entry.description()),
                            entry.amount().abs(),
                            entry.currency(),
                            entry.date()
                    );
                    return new CashflowIngestionService.CashflowIngestionCommand.IngestionItem(
                            transaction,
                            entry.externalReference().trim()
                    );
                })
                .toList();

        return cashflowIngestionService.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                command.profileId(),
                items
        ));
    }

    private static String descriptionFor(String counterpartyName, String description) {
        if (counterpartyName == null || counterpartyName.isBlank()) {
            return description;
        }
        return counterpartyName.trim() + " | " + description;
    }
}
