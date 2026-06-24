package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionFingerprintTest {

    @Test
    void computesDeterministicSha256FingerprintFromNormalizedFields() {
        var profileId = new ProfileId("p1");
        var transaction = transaction("Pago", "1000.00");

        var first = TransactionFingerprint.compute(profileId, transaction);
        var second = TransactionFingerprint.compute(profileId, transaction);

        assertThat(first)
                .isEqualTo("fp:v1:4480441486d2480c6bd52d41052f0814d6a50853787d3bd4540f71482fd6a056")
                .hasSize(70);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void normalizesWhitespaceAndNullDescriptionsToStableFingerprintInput() {
        var profileId = new ProfileId("p1");
        var compactWhitespace = TransactionFingerprint.compute(
                profileId,
                amount("1000.00"),
                Currency.getInstance("CLP"),
                LocalDate.of(2024, 6, 18),
                "  Pago\tcon\n espacios  "
        );
        var singleSpaces = TransactionFingerprint.compute(
                profileId,
                amount("1000.00"),
                Currency.getInstance("CLP"),
                LocalDate.of(2024, 6, 18),
                "Pago con espacios"
        );
        var nullDescription = TransactionFingerprint.compute(
                profileId,
                amount("1000.00"),
                Currency.getInstance("CLP"),
                LocalDate.of(2024, 6, 18),
                null
        );
        var blankDescription = TransactionFingerprint.compute(
                profileId,
                amount("1000.00"),
                Currency.getInstance("CLP"),
                LocalDate.of(2024, 6, 18),
                "   "
        );

        assertThat(compactWhitespace).isEqualTo(singleSpaces);
        assertThat(nullDescription).isEqualTo(blankDescription);
        assertThat(compactWhitespace).isNotEqualTo(blankDescription);
    }

    @Test
    void differentiatesMaterialFingerprintFieldsWithoutCollapsingBigDecimalScale() {
        var profileId = new ProfileId("p1");
        var base = TransactionFingerprint.compute(profileId, transaction("Pago", "1.0"));
        var differentAmountScale = TransactionFingerprint.compute(profileId, transaction("Pago", "1.00"));
        var differentDescription = TransactionFingerprint.compute(profileId, transaction("Pago proveedor", "1.0"));
        var differentDate = TransactionFingerprint.compute(
                profileId,
                new Transaction("Pago", amount("1.0"), Currency.getInstance("CLP"), LocalDate.of(2024, 6, 19))
        );
        var differentProfile = TransactionFingerprint.compute(new ProfileId("p2"), transaction("Pago", "1.0"));

        assertThat(base)
                .isNotEqualTo(differentAmountScale)
                .isNotEqualTo(differentDescription)
                .isNotEqualTo(differentDate)
                .isNotEqualTo(differentProfile);
    }

    @Test
    void excludesTransactionDirectionFromVersionOneFingerprint() {
        var profileId = new ProfileId("p1");
        var debit = transaction("Pago", "1000.00", TransactionDirection.DEBIT);
        var credit = transaction("Pago", "1000.00", TransactionDirection.CREDIT);

        var debitFingerprint = TransactionFingerprint.compute(profileId, debit);
        var creditFingerprint = TransactionFingerprint.compute(profileId, credit);

        assertThat(debitFingerprint).isEqualTo(creditFingerprint);
        assertThat(debitFingerprint).startsWith("fp:v1:");
    }

    private static Transaction transaction(String description, String amount) {
        return transaction(description, amount, TransactionDirection.CREDIT);
    }

    private static Transaction transaction(String description, String amount, TransactionDirection direction) {
        return new Transaction(
                description,
                amount(amount),
                Currency.getInstance("CLP"),
                LocalDate.of(2024, 6, 18),
                direction
        );
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
