package com.netkar.infrastructure;

import com.netkar.application.CalculateProductProfitabilityUseCase;
import com.netkar.domain.model.allocation.RevenueWeightedAllocation;
import com.netkar.domain.service.ProfitCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProfitabilityConfig {

    @Bean
    CalculateProductProfitabilityUseCase calculateProductProfitabilityUseCase() {
        return new CalculateProductProfitabilityUseCase(new ProfitCalculator(new RevenueWeightedAllocation()));
    }
}
