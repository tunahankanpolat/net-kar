package com.netkar.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class VatRate {

    public static final VatRate STANDARD = VatRate.of("0.20");

    private final BigDecimal value;

    private VatRate(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "value");
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("VAT rate must be within [0,1]: " + value);
        }
    }

    public static VatRate of(String value) {
        Objects.requireNonNull(value, "value");
        return new VatRate(new BigDecimal(value));
    }

    public BigDecimal value() {
        return value;
    }

    public BigDecimal onePlus() {
        return value.add(BigDecimal.ONE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VatRate vatRate)) return false;
        return value.compareTo(vatRate.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
