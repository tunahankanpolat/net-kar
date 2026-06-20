package com.netkar.application;

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
import com.netkar.domain.service.ProfitCalculator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculateProductProfitabilityUseCaseTest {

    private final CalculateProductProfitabilityUseCase useCase =
        new CalculateProductProfitabilityUseCase(new ProfitCalculator(new RevenueWeightedAllocation()));

    private SettlementPackage pkg(String id, String ref, String gross, Money commission) {
        SettlementLine line = new SettlementLine(ProductRef.of(ref), Quantity.of(1),
            Money.tryOf(gross), VatRate.of("0.20"), commission, Money.zeroTry(), Money.zeroTry());
        return new SettlementPackage(id, TransactionEffect.SALE, List.of(line),
            Money.zeroTry(), Money.zeroTry(), Money.zeroTry(), Money.zeroTry());
    }

    @Test
    void aggregates_per_product_across_packages_and_builds_red_list() {
        CostBook costs = CostBook.of(Map.of(
            ProductRef.of("WIN"), new ProductCost(Money.tryOf("10.00"), false, VatRate.of("0.20")),
            ProductRef.of("LOSE"), new ProductCost(Money.tryOf("90.00"), false, VatRate.of("0.20"))));

        var result = useCase.calculate(List.of(
            pkg("P1", "WIN", "100.00", Money.zeroTry()),
            pkg("P2", "WIN", "100.00", Money.zeroTry()),
            pkg("P3", "LOSE", "100.00", Money.tryOf("20.00"))), costs);

        assertThat(result.products()).hasSize(2);
        assertThat(result.redList().items()).hasSize(1);
        assertThat(result.redList().items().get(0).productRef()).isEqualTo(ProductRef.of("LOSE"));
        assertThat(result.redList().items().get(0).isLoss()).isTrue();
        assertThat(result.products().get(0).lineCount()).isEqualTo(2);
    }
}
