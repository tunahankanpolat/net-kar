package com.netkar.domain.result;

import java.util.List;

public record PackageProfit(String packageId, List<ProfitBreakdown> lines) {
    public PackageProfit(String packageId, List<ProfitBreakdown> lines) {
        this.packageId = packageId;
        this.lines = List.copyOf(lines);
    }
}
