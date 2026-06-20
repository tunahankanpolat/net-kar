package com.netkar.infrastructure;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.Quantity;
import com.netkar.domain.model.SettlementLine;
import com.netkar.domain.model.SettlementPackage;
import com.netkar.domain.model.TransactionEffect;
import com.netkar.domain.model.VatRate;
import com.netkar.domain.model.cost.CostBook;
import com.netkar.domain.model.cost.ProductCost;
import java.util.List;
import java.util.Map;

final class SampleData {

    private SampleData() {
    }

    static List<SettlementPackage> packages() {
        SettlementLine winner = new SettlementLine(
            ProductRef.of("WIN-1"), Quantity.of(1), Money.tryOf("120.00"), VatRate.of("0.20"),
            Money.tryOf("15.00"), Money.tryOf("1.00"), Money.zeroTry());
        SettlementLine loser = new SettlementLine(
            ProductRef.of("LOSE-1"), Quantity.of(1), Money.tryOf("100.00"), VatRate.of("0.20"),
            Money.tryOf("20.00"), Money.tryOf("0.80"), Money.zeroTry());

        return List.of(
            new SettlementPackage("PKG-1", TransactionEffect.SALE, List.of(winner),
                Money.tryOf("12.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry()),
            new SettlementPackage("PKG-2", TransactionEffect.SALE, List.of(loser),
                Money.tryOf("18.00"), Money.tryOf("8.49"), Money.zeroTry(), Money.zeroTry()));
    }

    static CostBook costs() {
        return CostBook.of(Map.of(
            ProductRef.of("WIN-1"), new ProductCost(Money.tryOf("60.00"), false, VatRate.of("0.20")),
            ProductRef.of("LOSE-1"), new ProductCost(Money.tryOf("85.00"), false, VatRate.of("0.20"))));
    }
}
