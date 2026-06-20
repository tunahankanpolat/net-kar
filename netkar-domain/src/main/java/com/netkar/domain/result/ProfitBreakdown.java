package com.netkar.domain.result;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.Percentage;
import com.netkar.domain.model.ProductRef;
import java.util.Optional;

public record ProfitBreakdown(
    ProductRef productRef,
    Money revenueNet,
    Money cogsNet,
    Money commission,
    Money serviceFeeShare,
    Money shippingShare,
    Money withholdingTax,
    Money campaignContribution,
    Money penaltyShare,
    Money earlyPaymentShare,
    EstimatedNetVatBurden estimatedNetVatBurden,
    Money netProfit,
    Optional<Percentage> margin,
    boolean allocated,
    boolean missingCost) {

    public boolean isLoss() {
        return netProfit.isNegative();
    }
}
