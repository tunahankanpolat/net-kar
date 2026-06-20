package com.netkar.infrastructure;

import com.netkar.application.CalculateProductProfitabilityUseCase;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.result.ProductProfitability;
import com.netkar.domain.service.ProfitCalculator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        CalculateProductProfitabilityUseCase useCase =
            new CalculateProductProfitabilityUseCase(new ProfitCalculator(new RevenueWeightedAllocation()));

        var result = useCase.calculate(SampleData.packages(), SampleData.costs());

        System.out.println("=== Ürün bazlı kârlılık ===");
        for (ProductProfitability p : result.products()) {
            System.out.printf("%-8s  netProfit=%s  vatBurden=%s  missingCost=%s%n",
                p.productRef().barcode(), p.totalNetProfit(), p.totalEstimatedVatBurden(),
                p.anyMissingCost());
        }

        System.out.println("=== Kırmızı liste (zarar edenler) ===");
        result.redList().items().forEach(p ->
            System.out.printf("%-8s  netProfit=%s%n", p.productRef().barcode(), p.totalNetProfit()));
    }
}
