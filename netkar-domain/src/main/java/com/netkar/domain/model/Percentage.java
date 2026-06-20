package com.netkar.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Percentage(BigDecimal value) {

    public static Percentage ratio(Money numerator, Money denominator) {
        if (denominator.isZero()) {
            throw new ArithmeticException("denominator must not be zero");
        }
        return new Percentage(
            numerator.amount().divide(denominator.amount(), 4, RoundingMode.HALF_UP));
    }
}
