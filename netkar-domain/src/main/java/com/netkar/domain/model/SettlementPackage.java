package com.netkar.domain.model;

import java.util.List;
import java.util.Objects;

public final class SettlementPackage {

    private final String packageId;
    private final TransactionEffect effect;
    private final List<SettlementLine> lines;
    private final Money shippingFee;
    private final Money serviceFee;
    private final Money penalty;
    private final Money earlyPaymentFee;

    public SettlementPackage(
        String packageId,
        TransactionEffect effect,
        List<SettlementLine> lines,
        Money shippingFee,
        Money serviceFee,
        Money penalty,
        Money earlyPaymentFee) {
        this.packageId = Objects.requireNonNull(packageId, "packageId");
        this.effect = Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("a settlement package must have at least one line");
        }
        this.lines = List.copyOf(lines);
        this.shippingFee = requireNonNegative(shippingFee, "shippingFee");
        this.serviceFee = requireNonNegative(serviceFee, "serviceFee");
        this.penalty = requireNonNegative(penalty, "penalty");
        this.earlyPaymentFee = requireNonNegative(earlyPaymentFee, "earlyPaymentFee");
    }

    private static Money requireNonNegative(Money money, String field) {
        Objects.requireNonNull(money, field);
        if (money.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return money;
    }

    public String packageId() { return packageId; }
    public TransactionEffect effect() { return effect; }
    public List<SettlementLine> lines() { return lines; }
    public Money shippingFee() { return shippingFee; }
    public Money serviceFee() { return serviceFee; }
    public Money penalty() { return penalty; }
    public Money earlyPaymentFee() { return earlyPaymentFee; }
}
