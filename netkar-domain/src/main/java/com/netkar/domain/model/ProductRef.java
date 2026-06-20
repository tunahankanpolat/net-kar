package com.netkar.domain.model;

import java.util.Objects;

public record ProductRef(String barcode) {
    public ProductRef {
        Objects.requireNonNull(barcode, "barcode");
        if (barcode.isBlank()) {
            throw new IllegalArgumentException("barcode must not be blank");
        }
    }

    public static ProductRef of(String barcode) {
        return new ProductRef(barcode);
    }
}
