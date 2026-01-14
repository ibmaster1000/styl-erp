package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class AgreementQuantitySummary {
    private final String colorCode;
    private final BigDecimal quantity;

    public AgreementQuantitySummary(String colorCode, BigDecimal quantity) {
        this.colorCode = colorCode;
        this.quantity = quantity;
    }

    public String getColorCode() {
        return colorCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
