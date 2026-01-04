package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductionAgreementDetailView {
    private final String styleNo;
    private final String agreementCode;
    private final List<ProductionAgreementDetailLine> lines;
    private final List<ProductionAgreementColorTotal> colorTotals;
    private final int grandQuantity;
    private final BigDecimal grandAmount;

    public ProductionAgreementDetailView(String styleNo,
                                         String agreementCode,
                                         List<ProductionAgreementDetailLine> lines,
                                         List<ProductionAgreementColorTotal> colorTotals,
                                         int grandQuantity,
                                         BigDecimal grandAmount) {
        this.styleNo = styleNo;
        this.agreementCode = agreementCode;
        this.lines = lines;
        this.colorTotals = colorTotals;
        this.grandQuantity = grandQuantity;
        this.grandAmount = grandAmount;
    }

    public String getStyleNo() {
        return styleNo;
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