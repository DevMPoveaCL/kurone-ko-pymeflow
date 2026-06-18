package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HexFormat;

final class TransactionFingerprint {
    private static final String PREFIX = "fp:v1:";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String FIELD_SEPARATOR = "|";

    private TransactionFingerprint() {
    }

    static String compute(ProfileId profileId, Transaction transaction) {
        return compute(
                profileId,
                transaction.amount(),
                transaction.currency(),
                transaction.bookedAt(),
                transaction.description()
        );
    }

    static String compute(ProfileId profileId, BigDecimal amount, Currency currency, LocalDate date, String description) {
        var input = String.join(
                FIELD_SEPARATOR,
                "pymeflow",
                "v1",
                profileId.value(),
                amount.toPlainString(),
                currency.getCurrencyCode().toUpperCase(),
                date.toString(),
                normalizeDescription(description)
        );
        return PREFIX + sha256Hex(input);
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        return description.trim().replaceAll("\\s+", " ");
    }

    private static String sha256Hex(String input) {
        try {
            var digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
