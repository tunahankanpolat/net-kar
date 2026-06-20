package com.netkar.domain.result;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import java.util.List;

public record ProductProfitability(
    ProductRef productRef,
    int lineCount,
    Money totalRevenueNet,
    Money totalNetProfit,
    Money totalEstimatedVatBurden,
    boolean anyMissingCost) {

    public static ProductProfitability from(ProductRef productRef, List<ProfitBreakdown> breakdowns) {
        Money revenue = Money.zeroTry();
        Money profit = Money.zeroTry();
        Money vat = Money.zeroTry();
        boolean missing = false;
        for (ProfitBreakdown b : breakdowns) {
            revenue = revenue.add(b.revenueNet());
            profit = profit.add(b.netProfit());
            vat = vat.add(b.estimatedNetVatBurden().net());
            missing = missing || b.missingCost();
        }
        return new ProductProfitability(
            productRef, breakdowns.size(), revenue, profit, vat, missing);
    }

    public boolean isLoss() {
        return totalNetProfit.isNegative();
    }
}
