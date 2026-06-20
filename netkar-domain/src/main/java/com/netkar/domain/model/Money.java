package com.netkar.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private static final Currency TRY = Currency.getInstance("TRY");
    private static final int MINOR_UNIT_SCALE = 2;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money tryOf(String amount) {
        return new Money(new BigDecimal(amount), TRY);
    }

    public static Money zeroTry() {
        return new Money(BigDecimal.ZERO, TRY);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    public VatSplit splitVat(VatRate rate) {
        Money gross = roundedToMinorUnit();
        java.math.BigDecimal vatAmount = gross.amount
            .multiply(rate.value())
            .divide(rate.onePlus(), 2, java.math.RoundingMode.HALF_UP);
        Money vat = new Money(vatAmount, currency);
        Money net = gross.subtract(vat);
        return new VatSplit(net, vat);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public Money roundedToMinorUnit() {
        return new Money(amount.setScale(MINOR_UNIT_SCALE, RoundingMode.HALF_UP), currency);
    }

    public int signum() {
        return amount.signum();
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return currency.equals(money.currency) && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return roundedToMinorUnit().amount + " " + currency.getCurrencyCode();
    }
}
