package com.netkar.infrastructure;

import com.netkar.application.CalculateProductProfitabilityUseCase;
import com.netkar.domain.result.ProductProfitability;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class DemoRunner implements CommandLineRunner {

    private final CalculateProductProfitabilityUseCase useCase;

    DemoRunner(CalculateProductProfitabilityUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void run(String... args) {
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
