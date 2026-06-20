package com.netkar.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Percentage(BigDecimal value) {

    public static Percentage ratio(Money numerator, Money denominator) {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (!numerator.currency().equals(denominator.currency())) {
            throw new IllegalArgumentException(
                "Currency mismatch: " + numerator.currency() + " vs " + denominator.currency());
        }
        if (denominator.isZero()) {
            throw new ArithmeticException("denominator must not be zero");
        }
        return new Percentage(
            numerator.amount().divide(denominator.amount(), 4, RoundingMode.HALF_UP));
    }
}
