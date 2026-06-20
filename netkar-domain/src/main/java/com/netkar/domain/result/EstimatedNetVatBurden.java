package com.netkar.domain.result;

import com.netkar.domain.model.Money;

public record EstimatedNetVatBurden(
    Money saleVat,
    Money cogsVat,
    Money shippingVat,
    Money commissionVat,
    Money serviceFeeVat,
    Money net) {

    public static EstimatedNetVatBurden of(
        Money saleVat, Money cogsVat, Money shippingVat, Money commissionVat, Money serviceFeeVat) {
        Money net = saleVat
            .subtract(cogsVat)
            .subtract(shippingVat)
            .subtract(commissionVat)
            .subtract(serviceFeeVat);
        return new EstimatedNetVatBurden(
            saleVat, cogsVat, shippingVat, commissionVat, serviceFeeVat, net);
    }
}
