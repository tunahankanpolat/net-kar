package com.netkar.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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

    /**
     * Splits this amount across the given weights using the largest-remainder method.
     * Returned parts are at minor-unit scale, in input order, and sum exactly to
     * {@code this.roundedToMinorUnit()}. A zero weight-sum falls back to an equal split.
     * Precondition: this amount must be non-negative (the SALE path only allocates
     * non-negative package fees); signed/return allocation is out of scope here.
     */
    public List<Money> allocate(List<BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("weights must not be empty");
        }
        if (isNegative()) {
            throw new IllegalArgumentException("allocation of a negative amount is not supported");
        }
        for (BigDecimal weight : weights) {
            Objects.requireNonNull(weight, "weight");
            if (weight.signum() < 0) {
                throw new IllegalArgumentException("weight must not be negative: " + weight);
            }
        }
        int n = weights.size();
        BigDecimal weightSum = weights.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean equalSplit = weightSum.signum() == 0;

        long totalCents = amount.setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2).longValueExact();

        long[] cents = new long[n];
        BigDecimal[] remainders = new BigDecimal[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            BigDecimal share = equalSplit
                ? BigDecimal.valueOf(totalCents)
                    .divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(totalCents).multiply(weights.get(i))
                    .divide(weightSum, 10, RoundingMode.HALF_UP);
            long floor = share.setScale(0, RoundingMode.FLOOR).longValueExact();
            cents[i] = floor;
            remainders[i] = share.subtract(BigDecimal.valueOf(floor));
            allocated += floor;
        }

        long leftover = totalCents - allocated;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> {
            int cmp = remainders[b].compareTo(remainders[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        for (int k = 0; k < leftover; k++) cents[order[k]] += 1;

        List<Money> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(new Money(BigDecimal.valueOf(cents[i], 2), currency));
        }
        return result;
    }
}
