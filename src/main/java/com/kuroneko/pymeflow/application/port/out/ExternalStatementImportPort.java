package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;

public interface ExternalStatementImportPort {
    CashflowIngestionService.CashflowIngestionResult importStatement(ExternalStatementImportCommand command);
}
