package com.netkar.domain.model.cost;

import com.netkar.domain.model.ProductRef;
import java.util.Map;
import java.util.Optional;

public final class CostBook {

    private final Map<ProductRef, ProductCost> costs;

    private CostBook(Map<ProductRef, ProductCost> costs) {
        this.costs = Map.copyOf(costs);
    }

    public static CostBook of(Map<ProductRef, ProductCost> costs) {
        return new CostBook(costs);
    }

    public Optional<ProductCost> find(ProductRef productRef) {
        return Optional.ofNullable(costs.get(productRef));
    }
}
