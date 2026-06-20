package com.netkar.domain.model.allocation;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.SettlementLine;
import java.math.BigDecimal;
import java.util.List;

public final class RevenueWeightedAllocation implements AllocationStrategy {

    @Override
    public List<Money> allocate(Money total, List<SettlementLine> lines) {
        List<BigDecimal> weights = lines.stream()
            .map(line -> line.lineGrossAmount().amount())
            .toList();
        return total.allocate(weights);
    }
}
