package com.netkar.domain.result;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record RedList(List<ProductProfitability> items) {

    public RedList(List<ProductProfitability> items) {
        this.items = List.copyOf(items);
    }

    public static RedList from(Collection<ProductProfitability> all) {
        Objects.requireNonNull(all, "all");
        List<ProductProfitability> losers = all.stream()
            .filter(ProductProfitability::isLoss)
            .sorted(Comparator.comparing(p -> p.totalNetProfit().amount()))
            .toList();
        return new RedList(losers);
    }
}
