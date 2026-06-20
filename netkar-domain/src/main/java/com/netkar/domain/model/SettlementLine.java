package com.netkar.domain.model;

import java.util.Objects;

public record SettlementLine(
    ProductRef productRef,
    Quantity quantity,
    Money lineGrossAmount,
    VatRate saleVatRate,
    Money commissionAmount,
    Money withholdingTax,
    Money campaignContribution) {

    public SettlementLine {
        Objects.requireNonNull(productRef, "productRef");
        Objects.requireNonNull(quantity, "quantity");
        requireNonNegative(lineGrossAmount, "lineGrossAmount");
        Objects.requireNonNull(saleVatRate, "saleVatRate");
        requireNonNegative(commissionAmount, "commissionAmount");
        requireNonNegative(withholdingTax, "withholdingTax");
        requireNonNegative(campaignContribution, "campaignContribution");
    }

    private static void requireNonNegative(Money money, String field) {
        Objects.requireNonNull(money, field);
        if (money.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }
}
