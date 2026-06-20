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

    @Test
    void subtracts_campaign_penalty_and_early_payment_fees() {
        // Line A carries a per-line campaign contribution; the package carries
        // penalty + early-payment fees that get revenue-weighted (75:25) across lines.
        SettlementLine a = new SettlementLine(ProductRef.of("A"), Quantity.of(1),
            Money.tryOf("75.00"), VatRate.of("0.20"), Money.zeroTry(), Money.zeroTry(),
            Money.tryOf("2.00")); // campaignContribution
        SettlementLine b = line("B", "25.00");
        SettlementPackage pkg = new SettlementPackage("PKG-1", TransactionEffect.SALE,
            List.of(a, b), Money.zeroTry(), Money.zeroTry(),
            Money.tryOf("10.00"), Money.tryOf("4.00")); // penalty, earlyPaymentFee

        PackageProfit result = calculator.calculate(pkg, CostBook.of(Map.of()));
        ProfitBreakdown ra = result.lines().get(0);
        ProfitBreakdown rb = result.lines().get(1);

        // penalty 10.00 -> 7.50 / 2.50 ; early 4.00 -> 3.00 / 1.00
        assertThat(ra.penaltyShare()).isEqualTo(Money.tryOf("7.50"));
        assertThat(ra.earlyPaymentShare()).isEqualTo(Money.tryOf("3.00"));
        assertThat(ra.campaignContribution()).isEqualTo(Money.tryOf("2.00"));
        // A: saleNet 62.50 - campaign 2.00 - penalty 7.50 - early 3.00 = 50.00
        assertThat(ra.netProfit()).isEqualTo(Money.tryOf("50.00"));
        // B: saleNet 20.83 - penalty 2.50 - early 1.00 = 17.33
        assertThat(rb.netProfit()).isEqualTo(Money.tryOf("17.33"));
    }
}
