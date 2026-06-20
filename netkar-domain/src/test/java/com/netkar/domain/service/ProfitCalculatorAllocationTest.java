package com.netkar.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitCalculatorAllocationTest {

    private final ProfitCalculator calculator = new ProfitCalculator(new RevenueWeightedAllocation());

    private SettlementLine line(String ref, String gross) {
        return new SettlementLine(ProductRef.of(ref), Quantity.of(1), Money.tryOf(gross),
            VatRate.of("0.20"), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void allocates_shipping_revenue_weighted_and_preserves_total() {
        SettlementPackage pkg = new SettlementPackage("PKG-1", TransactionEffect.SALE,
            List.of(line("A", "75.00"), line("B", "25.00")),
            Money.tryOf("40.00"), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());

        PackageProfit result = calculator.calculate(pkg, CostBook.of(Map.of()));
        ProfitBreakdown a = result.lines().get(0);
        ProfitBreakdown b = result.lines().get(1);

        assertThat(a.shippingShare()).isEqualTo(Money.tryOf("25.00")); // net part of 30.00 gross share
        assertThat(b.shippingShare()).isEqualTo(Money.tryOf("8.33"));
        assertThat(a.netProfit()).isEqualTo(Money.tryOf("37.50"));
        assertThat(b.netProfit()).isEqualTo(Money.tryOf("12.50"));
        assertThat(a.allocated()).isTrue();
        assertThat(a.missingCost()).isTrue();
    }
}
