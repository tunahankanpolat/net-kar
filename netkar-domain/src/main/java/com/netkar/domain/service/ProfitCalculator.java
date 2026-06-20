package com.netkar.domain.service;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.Percentage;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.VatSplit;
import com.netkar.domain.model.allocation.AllocationStrategy;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import com.netkar.domain.result.EstimatedNetVatBurden;
import com.netkar.domain.result.PackageProfit;
import com.netkar.domain.result.ProfitBreakdown;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProfitCalculator {

    private final AllocationStrategy allocationStrategy;

    public ProfitCalculator(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = Objects.requireNonNull(allocationStrategy, "allocationStrategy");
    }

    public PackageProfit calculate(SettlementPackage pkg, CostBook costs) {
        Objects.requireNonNull(pkg, "pkg");
        Objects.requireNonNull(costs, "costs");
        if (pkg.effect() != TransactionEffect.SALE) {
            throw new UnsupportedOperationException(
                "Only SALE is supported; " + pkg.effect()
                    + " arithmetic is deferred to the connector sub-project");
        }

        List<SettlementLine> lines = pkg.lines();
        List<Money> shipping = allocationStrategy.allocate(pkg.shippingFee(), lines);
        List<Money> service = allocationStrategy.allocate(pkg.serviceFee(), lines);
        List<Money> penalty = allocationStrategy.allocate(pkg.penalty(), lines);
        List<Money> early = allocationStrategy.allocate(pkg.earlyPaymentFee(), lines);

        boolean allocated = lines.size() > 1;
        List<ProfitBreakdown> breakdowns = new ArrayList<>(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            SettlementLine line = lines.get(i);

            VatSplit sale = line.lineGrossAmount().splitVat(line.saleVatRate());

            Optional<ProductCost> cost = costs.find(line.productRef());
            boolean missingCost = cost.isEmpty();
            Money cogsNet = cost.map(ProductCost::netAmount).orElse(Money.zeroTry());
            Money cogsVat = cost.map(ProductCost::vatAmount).orElse(Money.zeroTry());

            Money commissionNet = line.commissionAmount().roundedToMinorUnit();
            Money commissionVat = line.commissionAmount()
                .multiply(VatRate.STANDARD.value()).roundedToMinorUnit();

            VatSplit serviceSplit = service.get(i).splitVat(VatRate.STANDARD);
            VatSplit shippingSplit = shipping.get(i).splitVat(VatRate.STANDARD);

            EstimatedNetVatBurden vat = EstimatedNetVatBurden.of(
                sale.vat(), cogsVat, shippingSplit.vat(), commissionVat, serviceSplit.vat());

            Money netProfit = sale.net()
                .subtract(cogsNet)
                .subtract(commissionNet)
                .subtract(serviceSplit.net())
                .subtract(shippingSplit.net())
                .subtract(line.withholdingTax().roundedToMinorUnit())
                .subtract(line.campaignContribution().roundedToMinorUnit())
                .subtract(penalty.get(i))
                .subtract(early.get(i));

            Optional<Percentage> margin = sale.net().isZero()
                ? Optional.empty()
                : Optional.of(Percentage.ratio(netProfit, sale.net()));

            breakdowns.add(new ProfitBreakdown(
                line.productRef(), sale.net(), cogsNet, commissionNet,
                serviceSplit.net(), shippingSplit.net(),
                line.withholdingTax().roundedToMinorUnit(),
                line.campaignContribution().roundedToMinorUnit(),
                penalty.get(i), early.get(i), vat, netProfit, margin, allocated, missingCost));
        }

        return new PackageProfit(pkg.packageId(), breakdowns);
    }
}
