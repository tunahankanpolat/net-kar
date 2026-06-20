package com.netkar.domain.model;

public record Quantity(int value) {
    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("quantity must be >= 1: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
