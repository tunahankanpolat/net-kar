package com.netkar.domain.model.cost;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.VatRate;
import java.util.Objects;

public record ProductCost(Money amount, boolean vatInclusive, VatRate costVatRate) {

    public ProductCost {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(costVatRate, "costVatRate");
    }

    public Money netAmount() {
        return vatInclusive ? amount.splitVat(costVatRate).net() : amount.roundedToMinorUnit();
    }

    public Money vatAmount() {
        return vatInclusive
            ? amount.splitVat(costVatRate).vat()
            : amount.multiply(costVatRate.value()).roundedToMinorUnit();
    }
}
