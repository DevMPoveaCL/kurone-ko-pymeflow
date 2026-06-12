package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.Transaction;

import java.util.List;
import java.util.Locale;

public record SensitiveDataPolicy(List<String> blockedTerms) {
    public SensitiveDataPolicy {
        blockedTerms = List.copyOf(blockedTerms == null ? List.of() : blockedTerms.stream()
                .filter(term -> term != null && !term.isBlank())
                .map(term -> term.toLowerCase(Locale.ROOT))
                .toList());
    }

    public boolean rejects(Transaction transaction) {
        return rejectsText(transaction.description());
    }

    public boolean rejectsText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        var normalizedText = text.toLowerCase(Locale.ROOT);
        return blockedTerms.stream().anyMatch(normalizedText::contains);
    }
}
