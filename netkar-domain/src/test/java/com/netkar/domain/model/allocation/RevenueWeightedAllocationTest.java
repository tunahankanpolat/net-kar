package com.netkar.domain.model.allocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.VatRate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RevenueWeightedAllocationTest {

    private static SettlementLine line(String ref, String gross) {
        return new SettlementLine(ProductRef.of(ref), Quantity.of(1), Money.tryOf(gross),
            VatRate.STANDARD, Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void splits_total_by_revenue_weight_preserving_sum() {
        AllocationStrategy strategy = new RevenueWeightedAllocation();
        List<SettlementLine> lines = List.of(line("A", "75.00"), line("B", "25.00"));

        List<Money> shares = strategy.allocate(Money.tryOf("40.00"), lines);

        assertThat(shares).containsExactly(Money.tryOf("30.00"), Money.tryOf("10.00"));
        assertThat(shares.stream().reduce(Money.zeroTry(), Money::add)).isEqualTo(Money.tryOf("40.00"));
    }
}
