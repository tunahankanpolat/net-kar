package com.netkar.domain.model.cost;

import static org.assertj.core.api.Assertions.assertThat;

import com.netkar.domain.model.Money;
import com.netkar.domain.model.ProductRef;
import com.netkar.domain.model.VatRate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductCostTest {

    @Test
    void vat_inclusive_cost_is_split_into_net_and_vat() {
        ProductCost cost = new ProductCost(Money.tryOf("120.00"), true, VatRate.of("0.20"));
        assertThat(cost.netAmount()).isEqualTo(Money.tryOf("100.00"));
        assertThat(cost.vatAmount()).isEqualTo(Money.tryOf("20.00"));
    }

    @Test
    void vat_exclusive_cost_keeps_amount_as_net_and_adds_vat() {
        ProductCost cost = new ProductCost(Money.tryOf("100.00"), false, VatRate.of("0.20"));
        assertThat(cost.netAmount()).isEqualTo(Money.tryOf("100.00"));
        assertThat(cost.vatAmount()).isEqualTo(Money.tryOf("20.00"));
    }

    @Test
    void cost_book_finds_or_returns_empty() {
        ProductRef ref = ProductRef.of("A");
        CostBook book = CostBook.of(Map.of(ref, new ProductCost(Money.tryOf("10.00"), false, VatRate.STANDARD)));
        assertThat(book.find(ref)).isPresent();
        assertThat(book.find(ProductRef.of("MISSING"))).isEmpty();
    }
}
