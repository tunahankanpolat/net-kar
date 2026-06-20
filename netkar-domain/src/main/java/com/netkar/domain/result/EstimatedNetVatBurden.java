package com.netkar.domain.result;

import com.netkar.domain.model.Money;
import java.util.Objects;

public record EstimatedNetVatBurden(
    Money saleVat,
    Money cogsVat,
    Money shippingVat,
    Money commissionVat,
    Money serviceFeeVat,
    Money net) {

    public EstimatedNetVatBurden {
        Objects.requireNonNull(saleVat, "saleVat");
        Objects.requireNonNull(cogsVat, "cogsVat");
        Objects.requireNonNull(shippingVat, "shippingVat");
        Objects.requireNonNull(commissionVat, "commissionVat");
        Objects.requireNonNull(serviceFeeVat, "serviceFeeVat");
        Objects.requireNonNull(net, "net");
    }

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
