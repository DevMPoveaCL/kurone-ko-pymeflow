package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public record ManualReviewResolutionCommand(
        ProfileId profileId,
        String categoryKey,
        BigDecimal amount,
        Currency currency,
        LocalDate date,
        String description,
        String sourceReference,
        String sourceStatus,
        String outputStatus
) {
}
