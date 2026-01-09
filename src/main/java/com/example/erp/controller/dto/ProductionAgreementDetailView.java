package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductionAgreementDetailView {
	private final String styleCode;CodeNo,
                                         String agreementCode,
                                         List<ProductionAgreementDetailLine> lines,
                                         List<ProductionAgreementColorTotal> colorTotals,
                                         int grandQuantity,
                                         BigDecimal grandAmount) {
        this.styleCode = styleCode;
        this.agreementCode = agreementCode;
        this.lines = lines;
        this.colorTotals = colorTotals;
        this.grandQuantity = grandQuantity;
        this.grandAmount = grandAmount;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getAgreementCode() {
        return agreementCode;
    }

    public List<ProductionAgreementDetailLine> getLines() {
        return lines;
    }

    public List<ProductionAgreementColorTotal> getColorTotals() {
        return colorTotals;
    }

    public int getGrandQuantity() {
        return grandQuantity;
    }

    public BigDecimal getGrandAmount() {
        return grandAmount;
    }
}