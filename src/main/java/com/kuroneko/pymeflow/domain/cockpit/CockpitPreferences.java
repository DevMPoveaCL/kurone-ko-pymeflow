package com.kuroneko.pymeflow.domain.cockpit;

import java.math.BigDecimal;

public record CockpitPreferences(BigDecimal openingBalance, int preferredHorizonDays) {
    private static final int MAX_PRECISION = 18;
    private static final int MAX_SCALE = 2;

    public CockpitPreferences {
        if (openingBalance == null) {
            throw new IllegalArgumentException("El saldo inicial es obligatorio.");
        }
        if (openingBalance.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }
        if (integerDigits(openingBalance) > MAX_PRECISION - MAX_SCALE || Math.max(openingBalance.scale(), 0) > MAX_SCALE) {
            throw new IllegalArgumentException("El saldo inicial debe ser un valor seguro.");
        }
        if (preferredHorizonDays != 7 && preferredHorizonDays != 30) {
            throw new IllegalArgumentException("El horizonte debe ser 7 o 30 días.");
        }
    }

    private static int integerDigits(BigDecimal value) {
        return value.precision() - value.scale();
    }
}
