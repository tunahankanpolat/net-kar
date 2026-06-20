package com.netkar.domain.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import org.junit.jupiter.api.Test;

class EstimatedNetVatBurdenTest {

    @Test
    void net_is_output_vat_minus_all_input_vats() {
        EstimatedNetVatBurden vat = EstimatedNetVatBurden.of(
            Money.tryOf("20.00"),  // saleVat (output)
            Money.tryOf("10.00"),  // cogsVat
            Money.tryOf("2.00"),   // shippingVat
            Money.tryOf("3.00"),   // commissionVat
            Money.tryOf("1.70"));  // serviceFeeVat
        assertThat(vat.net()).isEqualTo(Money.tryOf("3.30"));
    }

    @Test
    void net_can_be_negative_meaning_carry_forward_vat_credit() {
        EstimatedNetVatBurden vat = EstimatedNetVatBurden.of(
            Money.tryOf("1.00"), Money.tryOf("2.00"), Money.zeroTry(),
            Money.zeroTry(), Money.zeroTry());
        assertThat(vat.net().isNegative()).isTrue();
    }
}
