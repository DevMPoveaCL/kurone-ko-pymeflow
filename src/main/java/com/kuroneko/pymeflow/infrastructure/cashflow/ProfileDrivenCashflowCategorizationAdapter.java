package com.kuroneko.pymeflow.infrastructure.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

@Component
public class ProfileDrivenCashflowCategorizationAdapter implements CashflowCategorizationPort {

    @Override
    public CategoryAssignment categorize(Transaction transaction, VerticalProfile profile) {
        var normalizedDescription = normalize(transaction.description());
        return profile.categories().stream()
                .filter(category -> matches(normalizedDescription, category, profile))
                .findFirst()
                .map(category -> new CategoryAssignment(Optional.of(category), false))
                .orElseGet(() -> new CategoryAssignment(Optional.empty(), true));
    }

    private static boolean matches(String normalizedDescription, CashflowCategory category, VerticalProfile profile) {
        return containsAnyToken(normalizedDescription, category.key())
                || containsAnyToken(normalizedDescription, category.displayName())
                || profile.rules().stream().anyMatch(rule -> rulePointsToCategory(rule, category)
                        && containsAnyToken(normalizedDescription, rule.ruleKey()));
    }

    private static boolean rulePointsToCategory(ProfileRule rule, CashflowCategory category) {
        return containsAnyToken(normalize(rule.actionKey()), category.key())
                || containsAnyToken(normalize(rule.actionKey()), category.displayName())
                || containsAnyToken(normalize(rule.ruleKey()), category.key())
                || containsAnyToken(normalize(rule.ruleKey()), category.displayName());
    }

    private static boolean containsAnyToken(String normalizedDescription, String text) {
        for (String token : normalize(text).split(" ")) {
            if (!token.isBlank() && (normalizedDescription.contains(token) || normalizedDescription.contains(singular(token)))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        var withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String singular(String token) {
        return token.length() > 3 && token.endsWith("s") ? token.substring(0, token.length() - 1) : token;
    }
}
