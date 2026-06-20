package com.netkar.domain.result;

import java.util.List;
import java.util.Objects;

public record PackageProfit(String packageId, List<ProfitBreakdown> lines) {
    public PackageProfit {
        Objects.requireNonNull(packageId, "packageId");
        lines = List.copyOf(lines);
    }
}
