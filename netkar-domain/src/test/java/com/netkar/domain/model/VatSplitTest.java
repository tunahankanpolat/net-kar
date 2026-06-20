package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VatSplitTest {

    @Test
    void splits_gross_into_net_and_vat_at_20_percent() {
        VatSplit split = Money.tryOf("120.00").splitVat(VatRate.of("0.20"));
        assertThat(split.net()).isEqualTo(Money.tryOf("100.00"));
        assertThat(split.vat()).isEqualTo(Money.tryOf("20.00"));
    }

    @Test
    void net_plus_vat_reconciles_to_gross_to_the_kurus() {
        Money gross = Money.tryOf("19.99");
        VatSplit split = gross.splitVat(VatRate.of("0.10"));
        assertThat(split.net().add(split.vat())).isEqualTo(gross);
    }

    @Test
    void zero_rate_yields_zero_vat() {
        VatSplit split = Money.tryOf("50.00").splitVat(VatRate.of("0.00"));
        assertThat(split.net()).isEqualTo(Money.tryOf("50.00"));
        assertThat(split.vat()).isEqualTo(Money.zeroTry());
    }

    @Test
    void rejects_rate_outside_zero_to_one() {
        assertThatThrownBy(() -> VatRate.of("1.50")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VatRate.of("-0.10")).isInstanceOf(IllegalArgumentException.class);
    }
}
