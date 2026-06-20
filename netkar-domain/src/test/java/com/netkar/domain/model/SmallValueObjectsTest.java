package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class SmallValueObjectsTest {

    @Test
    void product_ref_rejects_blank() {
        assertThatThrownBy(() -> ProductRef.of("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(ProductRef.of("BARCODE-1").barcode()).isEqualTo("BARCODE-1");
    }

    @Test
    void quantity_must_be_positive() {
        assertThatThrownBy(() -> Quantity.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Quantity.of(3).value()).isEqualTo(3);
    }

    @Test
    void percentage_is_ratio_of_two_money_values() {
        Percentage margin = Percentage.ratio(Money.tryOf("23.00"), Money.tryOf("100.00"));
        assertThat(margin.value()).isEqualByComparingTo(new BigDecimal("0.2300"));
    }

    @Test
    void percentage_rejects_zero_denominator() {
        assertThatThrownBy(() -> Percentage.ratio(Money.tryOf("1.00"), Money.zeroTry()))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void percentage_rejects_currency_mismatch() {
        Money usd = Money.of(new BigDecimal("100.00"), Currency.getInstance("USD"));
        assertThatThrownBy(() -> Percentage.ratio(Money.tryOf("23.00"), usd))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transaction_effect_has_three_values() {
        assertThat(TransactionEffect.values())
            .containsExactly(TransactionEffect.SALE, TransactionEffect.RETURN, TransactionEffect.CANCEL);
    }
}
