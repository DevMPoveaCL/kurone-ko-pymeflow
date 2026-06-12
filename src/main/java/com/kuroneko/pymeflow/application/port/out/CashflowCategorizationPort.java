package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;

public interface CashflowCategorizationPort {
    CategoryAssignment categorize(Transaction transaction, VerticalProfile profile);
}
