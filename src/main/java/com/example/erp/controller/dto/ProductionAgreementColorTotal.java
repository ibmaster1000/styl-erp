package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class ProductionAgreementColorTotal {
    private final String color;
    private final int totalQuantity;
    private final BigDecimal totalAmount;

    public ProductionAgreementColorTotal(String color, int totalQuantity, BigDecimal totalAmount) {
        this.color = color;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    public String getColor() {
        return color;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}