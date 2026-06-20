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
import com.netkar.domain.model.cost.ProductCost;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitCalculatorEdgeCasesTest {

    private final ProfitCalculator calculator = new ProfitCalculator(new RevenueWeightedAllocation());

    private SettlementPackage singleLine(String gross, String vatRate, Money commission) {
        SettlementLine line = new SettlementLine(ProductRef.of("A"), Quantity.of(1),
            Money.tryOf(gross), VatRate.of(vatRate), commission, Money.zeroTry(), Money.zeroTry());
        return new SettlementPackage("PKG", TransactionEffect.SALE, List.of(line),
            Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void missing_cost_flags_line_and_treats_cogs_as_zero() {
        ProfitBreakdown b = calculator.calculate(
            singleLine("100.00", "0.20", Money.zeroTry()), CostBook.of(Map.of())).lines().get(0);
        assertThat(b.missingCost()).isTrue();
        assertThat(b.cogsNet()).isEqualTo(Money.zeroTry());
    }

    @Test
    void zero_vat_rate_yields_zero_sale_vat() {
        ProfitBreakdown b = calculator.calculate(
            singleLine("100.00", "0.00", Money.zeroTry()), CostBook.of(Map.of())).lines().get(0);
        assertThat(b.revenueNet()).isEqualTo(Money.tryOf("100.00"));
        assertThat(b.estimatedNetVatBurden().saleVat()).isEqualTo(Money.zeroTry());
    }

    @Test
    void vat_inclusive_cost_with_different_cost_vat_rate_affects_net_profit() {
        // sale gross 200.00 @20% -> net 166.67 ; cost 110.00 KDV-dahil @10% -> cogsNet 100.00
        CostBook costs = CostBook.of(Map.of(
            ProductRef.of("A"), new ProductCost(Money.tryOf("110.00"), true, VatRate.of("0.10"))));
        ProfitBreakdown b = calculator.calculate(
            singleLine("200.00", "0.20", Money.zeroTry()), costs).lines().get(0);
        assertThat(b.cogsNet()).isEqualTo(Money.tryOf("100.00"));
        assertThat(b.netProfit()).isEqualTo(Money.tryOf("66.67")); // 166.67 - 100.00
    }

    @Test
    void loss_making_line_is_marked_as_loss() {
        CostBook costs = CostBook.of(Map.of(
            ProductRef.of("A"), new ProductCost(Money.tryOf("90.00"), false, VatRate.of("0.20"))));
        ProfitBreakdown b = calculator.calculate(
            singleLine("100.00", "0.20", Money.tryOf("20.00")), costs).lines().get(0);
        // net 83.33 - cogs 90.00 - commission 20.00 = -26.67
        assertThat(b.netProfit().isNegative()).isTrue();
        assertThat(b.isLoss()).isTrue();
    }
}
