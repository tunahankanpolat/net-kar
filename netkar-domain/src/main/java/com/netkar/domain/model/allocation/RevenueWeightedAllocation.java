package com.netkar.domain.model.allocation;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.SettlementLine;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class RevenueWeightedAllocation implements AllocationStrategy {

    @Override
    public List<Money> allocate(Money total, List<SettlementLine> lines) {
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(lines, "lines");
        List<BigDecimal> weights = lines.stream()
            .map(line -> line.lineGrossAmount().amount())
            .toList();
        return total.allocate(weights);
    }
}
