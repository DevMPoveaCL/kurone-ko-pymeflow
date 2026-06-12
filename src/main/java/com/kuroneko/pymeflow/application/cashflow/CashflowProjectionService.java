package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ObligationTemplate;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CashflowProjectionService {
    private static final Period MONTHLY = Period.ofMonths(1);
    private static final String PROJECTED_BALANCE_BELOW_THRESHOLD = "projected_balance_below_threshold";
    private static final String PROJECTED_BALANCE_ABOVE_THRESHOLD = "projected_balance_above_threshold";
    private static final String OBLIGATIONS_DUE_BEFORE_CASH_INFLOW = "obligations_due_before_cash_inflow";

    private final VerticalProfileService verticalProfileService;

    public CashflowProjectionService(VerticalProfileService verticalProfileService) {
        this.verticalProfileService = verticalProfileService;
    }

    public CashflowProjectionResult project(CashflowProjectionCommand command) {
        var profile = verticalProfileService.loadProfile(command.profileId());
        var categories = profile.categories().stream()
                .collect(Collectors.toMap(CashflowCategory::key, Function.identity()));
        validateTransactions(command, categories);

        var dailyBalances = new ArrayList<DailyProjectedBalance>();
        var appliedObligations = new ArrayList<AppliedObligation>();
        var alerts = new ArrayList<ProjectionAlert>();
        var runningBalance = command.openingBalance();

        for (int offset = 0; offset < command.horizonDays(); offset++) {
            var date = command.startDate().plusDays(offset);
            var dailyTransactions = transactionsOn(command.transactions(), date);
            var inflows = sumByDirection(dailyTransactions, categories, CashflowDirection.INFLOW);
            var outflows = sumByDirection(dailyTransactions, categories, CashflowDirection.OUTFLOW);
            var dueObligations = obligationsDueOn(profile.obligations(), date);
            var obligations = dueObligations.stream()
                    .map(ObligationTemplate::estimatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (ObligationTemplate obligation : dueObligations) {
                appliedObligations.add(new AppliedObligation(
                        obligation.key(),
                        obligation.displayName(),
                        date,
                        obligation.estimatedAmount()
                ));
            }

            runningBalance = runningBalance.add(inflows).subtract(outflows).subtract(obligations);
            var dailyBalance = new DailyProjectedBalance(date, inflows, outflows, obligations, runningBalance);
            dailyBalances.add(dailyBalance);
            alerts.addAll(alertsFor(profile.rules(), dailyBalance));
        }

        return new CashflowProjectionResult(
                dailyBalances,
                dailyBalances.getLast().balance(),
                appliedObligations,
                alerts
        );
    }

    private static void validateTransactions(
            CashflowProjectionCommand command,
            Map<String, CashflowCategory> categories
    ) {
        for (ProjectedCashflowTransaction transaction : command.transactions()) {
            if (!categories.containsKey(transaction.categoryKey())) {
                throw new IllegalArgumentException("Unknown category: " + transaction.categoryKey());
            }
            if (!command.currency().equals(transaction.currency())) {
                throw new IllegalArgumentException("Transaction currency must match projection currency");
            }
            if (transaction.date().isBefore(command.startDate())
                    || !transaction.date().isBefore(command.startDate().plusDays(command.horizonDays()))) {
                throw new IllegalArgumentException("Transaction date must be within projection horizon");
            }
        }
    }

    private static List<ProjectedCashflowTransaction> transactionsOn(
            List<ProjectedCashflowTransaction> transactions,
            LocalDate date
    ) {
        return transactions.stream()
                .filter(transaction -> transaction.date().equals(date))
                .toList();
    }

    private static BigDecimal sumByDirection(
            List<ProjectedCashflowTransaction> transactions,
            Map<String, CashflowCategory> categories,
            CashflowDirection direction
    ) {
        return transactions.stream()
                .filter(transaction -> categories.get(transaction.categoryKey()).direction() == direction)
                .map(ProjectedCashflowTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<ObligationTemplate> obligationsDueOn(List<ObligationTemplate> obligations, LocalDate date) {
        return obligations.stream()
                .filter(obligation -> MONTHLY.equals(obligation.frequency()))
                .filter(obligation -> dueDateFor(obligation, YearMonth.from(date)).equals(date))
                .toList();
    }

    private static LocalDate dueDateFor(ObligationTemplate obligation, YearMonth month) {
        return month.atDay(Math.min(obligation.dueDayOfMonth(), month.lengthOfMonth()));
    }

    private static List<ProjectionAlert> alertsFor(List<ProfileRule> rules, DailyProjectedBalance balance) {
        return rules.stream()
                .filter(rule -> triggers(rule, balance))
                .map(rule -> new ProjectionAlert(
                        rule.ruleKey(),
                        rule.actionKey(),
                        rule.condition(),
                        balance.date(),
                        balance.balance()
                ))
                .toList();
    }

    private static boolean triggers(ProfileRule rule, DailyProjectedBalance balance) {
        return switch (rule.condition()) {
            case PROJECTED_BALANCE_BELOW_THRESHOLD -> balance.balance().compareTo(rule.threshold()) < 0;
            case PROJECTED_BALANCE_ABOVE_THRESHOLD -> balance.balance().compareTo(rule.threshold()) > 0;
            case OBLIGATIONS_DUE_BEFORE_CASH_INFLOW ->
                    balance.obligations().compareTo(rule.threshold()) > 0 && balance.inflows().signum() == 0;
            default -> false;
        };
    }
}
