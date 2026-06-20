package com.netkar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyAllocateTest {

    private static final BigDecimal ONE = BigDecimal.ONE;

    @Test
    void distributes_remainder_so_parts_sum_to_total() {
        List<Money> parts = Money.tryOf("100.00").allocate(List.of(ONE, ONE, ONE));
        assertThat(parts).containsExactly(
            Money.tryOf("33.34"), Money.tryOf("33.33"), Money.tryOf("33.33"));
        assertThat(parts.stream().reduce(Money.zeroTry(), Money::add)).isEqualTo(Money.tryOf("100.00"));
    }

    @Test
    void allocates_proportional_to_weights() {
        List<Money> parts = Money.tryOf("60.00")
            .allocate(List.of(new BigDecimal("30"), new BigDecimal("10")));
        assertThat(parts).containsExactly(Money.tryOf("45.00"), Money.tryOf("15.00"));
    }

    @Test
    void zero_weight_sum_falls_back_to_equal_split() {
        List<Money> parts = Money.tryOf("10.00")
            .allocate(List.of(BigDecimal.ZERO, BigDecimal.ZERO));
        assertThat(parts).containsExactly(Money.tryOf("5.00"), Money.tryOf("5.00"));
        assertThat(parts.stream().reduce(Money.zeroTry(), Money::add)).isEqualTo(Money.tryOf("10.00"));
    }
}
