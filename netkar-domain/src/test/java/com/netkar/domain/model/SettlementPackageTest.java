package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SettlementPackageTest {

    private static SettlementLine line() {
        return new SettlementLine(
            ProductRef.of("A"), Quantity.of(1), Money.tryOf("100.00"), VatRate.STANDARD,
            Money.tryOf("15.00"), Money.tryOf("0.80"), Money.zeroTry());
    }

    @Test
    void builds_a_valid_sale_package() {
        SettlementPackage pkg = new SettlementPackage(
            "PKG-1", TransactionEffect.SALE, List.of(line()),
            Money.tryOf("30.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry());
        assertThat(pkg.lines()).hasSize(1);
        assertThat(pkg.effect()).isEqualTo(TransactionEffect.SALE);
    }

    @Test
    void rejects_empty_lines() {
        assertThatThrownBy(() -> new SettlementPackage(
            "PKG-1", TransactionEffect.SALE, List.of(),
            Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_negative_package_fee() {
        assertThatThrownBy(() -> new SettlementPackage(
            "PKG-1", TransactionEffect.SALE, List.of(line()),
            Money.tryOf("-1.00"), Money.zeroTry(), Money.zeroTry(), Money.zeroTry()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
