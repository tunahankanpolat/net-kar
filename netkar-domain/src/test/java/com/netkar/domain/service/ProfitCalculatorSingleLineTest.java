package com.netkar.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfitCalculatorSingleLineTest {

    private final ProfitCalculator calculator = new ProfitCalculator(new RevenueWeightedAllocation());

    private SettlementPackage salePackage() {
        SettlementLine line = new SettlementLine(
            ProductRef.of("A"), Quantity.of(1), Money.tryOf("120.00"), VatRate.of("0.20"),
            Money.tryOf("15.00"), Money.tryOf("1.00"), Money.zeroTry());
        return new SettlementPackage("PKG-1", TransactionEffect.SALE, List.of(line),
            Money.tryOf("12.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry());
    }

    private CostBook costs() {
        return CostBook.of(Map.of(
            ProductRef.of("A"), new ProductCost(Money.tryOf("60.00"), false, VatRate.of("0.20"))));
    }

    @Test
    void computes_net_profit_and_vat_burden_for_one_line() {
        PackageProfit result = calculator.calculate(salePackage(), costs());
        ProfitBreakdown b = result.lines().get(0);

        assertThat(b.revenueNet()).isEqualTo(Money.tryOf("100.00"));
        assertThat(b.cogsNet()).isEqualTo(Money.tryOf("60.00"));
        assertThat(b.commission()).isEqualTo(Money.tryOf("15.00"));
        assertThat(b.shippingShare()).isEqualTo(Money.tryOf("10.00"));
        assertThat(b.serviceFeeShare()).isEqualTo(Money.tryOf("7.07"));
        assertThat(b.estimatedNetVatBurden().net()).isEqualTo(Money.tryOf("1.58"));
        assertThat(b.netProfit()).isEqualTo(Money.tryOf("6.93"));
        assertThat(b.missingCost()).isFalse();
        assertThat(b.margin()).isPresent();
        assertThat(b.allocated()).isFalse();
    }

    @Test
    void rejects_non_sale_effect_as_deferred() {
        SettlementLine line = new SettlementLine(
            ProductRef.of("A"), Quantity.of(1), Money.tryOf("120.00"), VatRate.of("0.20"),
            Money.tryOf("15.00"), Money.zeroTry(), Money.zeroTry());
        SettlementPackage returnPkg = new SettlementPackage("PKG-2", TransactionEffect.RETURN,
            List.of(line), Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());

        assertThatThrownBy(() -> calculator.calculate(returnPkg, costs()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("RETURN");
    }
}
