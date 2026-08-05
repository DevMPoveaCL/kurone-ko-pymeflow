package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ObligationTemplate;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashflowProjectionServiceTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");

    @Test
    void projectsDailyBalancesFromOpeningBalanceAndCategorizedMovements() {
        var service = service(profile(List.of(), List.of()));
        var command = command(
                BigDecimal.valueOf(1_000),
                LocalDate.of(2026, 1, 1),
                3,
                List.of(
                        transaction("sales", 500, LocalDate.of(2026, 1, 1)),
                        transaction("suppliers", 200, LocalDate.of(2026, 1, 2)),
                        transaction("transfer", 999, LocalDate.of(2026, 1, 2))
                )
        );

        var result = service.project(command);

        assertThat(result.dailyBalances()).extracting(DailyProjectedBalance::balance)
                .containsExactly(
                        BigDecimal.valueOf(1_500),
                        BigDecimal.valueOf(1_300),
                        BigDecimal.valueOf(1_300)
                );
        assertThat(result.dailyBalances().get(0).inflows()).isEqualByComparingTo("500");
        assertThat(result.dailyBalances().get(1).outflows()).isEqualByComparingTo("200");
        assertThat(result.closingProjectedBalance()).isEqualByComparingTo("1300");
    }

    @Test
    void appliesMonthlyProfileObligationsToDueDateBalances() {
        var obligation = new ObligationTemplate(
                "rent",
                "Rent",
                BigDecimal.valueOf(300),
                Period.ofMonths(1),
                5
        );
        var service = service(profile(List.of(), List.of(obligation)));

        var result = service.project(command(
                BigDecimal.valueOf(1_000),
                LocalDate.of(2026, 1, 4),
                3,
                List.of()
        ));

        assertThat(result.dailyBalances()).extracting(DailyProjectedBalance::obligations)
                .containsExactly(BigDecimal.ZERO, BigDecimal.valueOf(300), BigDecimal.ZERO);
        assertThat(result.dailyBalances()).extracting(DailyProjectedBalance::balance)
                .containsExactly(BigDecimal.valueOf(1_000), BigDecimal.valueOf(700), BigDecimal.valueOf(700));
        assertThat(result.appliedObligations()).singleElement()
                .satisfies(applied -> {
                    assertThat(applied.obligationKey()).isEqualTo("rent");
                    assertThat(applied.dueDate()).isEqualTo(LocalDate.of(2026, 1, 5));
                    assertThat(applied.amount()).isEqualByComparingTo("300");
                });
    }

    @Test
    void appliesShortMonthDueDayOnLastValidDay() {
        var obligation = new ObligationTemplate(
                "payroll",
                "Payroll",
                BigDecimal.valueOf(400),
                Period.ofMonths(1),
                31
        );
        var service = service(profile(List.of(), List.of(obligation)));

        var result = service.project(command(
                BigDecimal.valueOf(1_000),
                LocalDate.of(2026, 2, 27),
                2,
                List.of()
        ));

        assertThat(result.appliedObligations()).singleElement()
                .extracting(AppliedObligation::dueDate)
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(result.closingProjectedBalance()).isEqualByComparingTo("600");
    }

    @Test
    void createsAlertsForSupportedProfileRulesAndIgnoresUnknownConditions() {
        var rules = List.of(
                new ProfileRule("low", "projected_balance_below_threshold", BigDecimal.valueOf(250_000), "warn-low"),
                new ProfileRule("high", "projected_balance_above_threshold", BigDecimal.valueOf(750_000), "mark-healthy"),
                new ProfileRule("unknown", "future_condition", BigDecimal.ZERO, "ignore")
        );
        var service = service(profile(rules, List.of()));

        var result = service.project(command(
                BigDecimal.valueOf(100_000),
                LocalDate.of(2026, 1, 1),
                2,
                List.of(
                        transaction("sales", 700_000, LocalDate.of(2026, 1, 1)),
                        transaction("suppliers", 600_000, LocalDate.of(2026, 1, 2))
                )
        ));

        assertThat(result.alerts()).extracting(ProjectionAlert::ruleKey)
                .containsExactly("high", "low");
        assertThat(result.alerts()).filteredOn(alert -> alert.ruleKey().equals("high"))
                .singleElement()
                .satisfies(alert -> assertThat(alert.threshold()).isEqualByComparingTo("750000"));
        assertThat(result.alerts()).filteredOn(alert -> alert.ruleKey().equals("low"))
                .singleElement()
                .satisfies(alert -> assertThat(alert.threshold()).isEqualByComparingTo("250000"));
    }

    @Test
    void createsObligationTimingAlertWhenObligationIsDueWithoutSameDayInflow() {
        var rule = new ProfileRule("due-before-inflow", "obligations_due_before_cash_inflow", BigDecimal.ZERO, "warn-due");
        var obligation = new ObligationTemplate("tax", "Tax", BigDecimal.valueOf(100), Period.ofMonths(1), 3);
        var service = service(profile(List.of(rule), List.of(obligation)));

        var result = service.project(command(
                BigDecimal.valueOf(1_000),
                LocalDate.of(2026, 1, 3),
                1,
                List.of()
        ));

        assertThat(result.alerts()).singleElement()
                .extracting(ProjectionAlert::ruleKey)
                .isEqualTo("due-before-inflow");
    }

    @Test
    void rejectsUnknownCategoriesProfilesAndCurrencyMismatches() {
        var service = service(profile(List.of(), List.of()));

        assertThatThrownBy(() -> service.project(command(
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                1,
                List.of(transaction("unknown", 100, LocalDate.of(2026, 1, 1)))
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown category");

        assertThatThrownBy(() -> service.project(new CashflowProjectionCommand(
                PROFILE_ID,
                BigDecimal.ZERO,
                CLP,
                LocalDate.of(2026, 1, 1),
                1,
                List.of(new ProjectedCashflowTransaction(
                        "sales",
                        BigDecimal.valueOf(100),
                        Currency.getInstance("USD"),
                        LocalDate.of(2026, 1, 1)
                ))
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

        var unknownProfileService = new CashflowProjectionService(new VerticalProfileService(id -> Optional.empty()));
        assertThatThrownBy(() -> unknownProfileService.project(command(
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                1,
                List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");
    }

    private static CashflowProjectionService service(VerticalProfile profile) {
        return new CashflowProjectionService(new VerticalProfileService(id -> Optional.of(profile)));
    }

    private static CashflowProjectionCommand command(
            BigDecimal openingBalance,
            LocalDate startDate,
            int horizonDays,
            List<ProjectedCashflowTransaction> transactions
    ) {
        return new CashflowProjectionCommand(PROFILE_ID, openingBalance, CLP, startDate, horizonDays, transactions);
    }

    private static ProjectedCashflowTransaction transaction(String categoryKey, long amount, LocalDate date) {
        return new ProjectedCashflowTransaction(categoryKey, BigDecimal.valueOf(amount), CLP, date);
    }

    private static VerticalProfile profile(List<ProfileRule> rules, List<ObligationTemplate> obligations) {
        return new VerticalProfile(
                PROFILE_ID,
                "Retail",
                rules,
                List.of(
                        new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW),
                        new CashflowCategory("suppliers", "Suppliers", CashflowDirection.OUTFLOW),
                        new CashflowCategory("transfer", "Transfer", CashflowDirection.TRANSFER)
                ),
                obligations
        );
    }
}
