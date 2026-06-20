package com.netkar.application;

import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProductProfitability;
import com.netkar.domain.result.ProfitBreakdown;
import com.netkar.domain.result.RedList;
import com.netkar.domain.service.ProfitCalculator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CalculateProductProfitabilityUseCase {

    private final ProfitCalculator calculator;

    public CalculateProductProfitabilityUseCase(ProfitCalculator calculator) {
        this.calculator = calculator;
    }

    public Result calculate(List<SettlementPackage> packages, CostBook costs) {
        Map<ProductRef, List<ProfitBreakdown>> byProduct = new LinkedHashMap<>();
        for (SettlementPackage pkg : packages) {
            PackageProfit profit = calculator.calculate(pkg, costs);
            for (ProfitBreakdown breakdown : profit.lines()) {
                byProduct.computeIfAbsent(breakdown.productRef(), key -> new ArrayList<>())
                    .add(breakdown);
            }
        }

        List<ProductProfitability> products = new ArrayList<>();
        byProduct.forEach((ref, breakdowns) ->
            products.add(ProductProfitability.from(ref, breakdowns)));

        return new Result(List.copyOf(products), RedList.from(products));
    }

    public record Result(List<ProductProfitability> products, RedList redList) {
    }
}
