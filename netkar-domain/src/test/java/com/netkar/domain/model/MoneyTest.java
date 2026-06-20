package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void defaults_to_try() {
        assertThat(Money.tryOf("10.00").currency()).isEqualTo(Currency.getInstance("TRY"));
    }

    @Test
    void equality_ignores_trailing_zero_scale() {
        assertThat(Money.tryOf("10.0")).isEqualTo(Money.tryOf("10.00"));
        assertThat(Money.tryOf("10.0")).hasSameHashCodeAs(Money.tryOf("10.00"));
    }

    @Test
    void adds_and_subtracts() {
        assertThat(Money.tryOf("10.00").add(Money.tryOf("2.50"))).isEqualTo(Money.tryOf("12.50"));
        assertThat(Money.tryOf("10.00").subtract(Money.tryOf("2.50"))).isEqualTo(Money.tryOf("7.50"));
    }

    @Test
    void multiplies_keeping_precision() {
        assertThat(Money.tryOf("10.00").multiply(new BigDecimal("0.20")))
            .isEqualTo(Money.tryOf("2.00"));
    }

    @Test
    void allows_negative_and_reports_sign() {
        assertThat(Money.tryOf("-1.00").isNegative()).isTrue();
        assertThat(Money.zeroTry().isZero()).isTrue();
        assertThat(Money.tryOf("1.00").isPositive()).isTrue();
    }

    @Test
    void rounds_to_minor_unit_half_up() {
        assertThat(Money.tryOf("1.005").roundedToMinorUnit()).isEqualTo(Money.tryOf("1.01"));
    }

    @Test
    void rejects_mixed_currency_arithmetic() {
        Money usd = Money.of(new BigDecimal("1.00"), Currency.getInstance("USD"));
        assertThatThrownBy(() -> Money.tryOf("1.00").add(usd))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
