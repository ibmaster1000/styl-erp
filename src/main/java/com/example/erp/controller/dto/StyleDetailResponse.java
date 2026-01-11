package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StyleDetailResponse {

    private final Long stylesId;
    private final String styleCode;
    private final String itemCode;
    private final String itemName;
    private final String designerEmpNo;
    private final String productEmpNo;
    private final String salesEmpNo;
    private final String logisticEmpNo;
    private final LocalDate startDate;
    private final BigDecimal productionCost;
    private final BigDecimal supplyPrice;
    private final BigDecimal salesPrice;
    private final Integer isActive;
    private final List<String> colorCodes;
    private final List<String> sizeCodes;
    private final List<StyleRuleCodeView> colorRules;
    private final List<StyleRuleCodeView> sizeRules;

    public StyleDetailResponse(Long stylesId, String styleCode, String itemCode, String itemName,
            String designerEmpNo, String productEmpNo, String salesEmpNo, String logisticEmpNo,
            LocalDate startDate, BigDecimal productionCost, BigDecimal supplyPrice,
            BigDecimal salesPrice, Integer isActive, List<String> colorCodes, List<String> sizeCodes,
            List<StyleRuleCodeView> colorRules, List<StyleRuleCodeView> sizeRules) {
        this.stylesId = stylesId;
        this.styleCode = styleCode;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.designerEmpNo = designerEmpNo;
        this.productEmpNo = productEmpNo;
        this.salesEmpNo = salesEmpNo;
        this.logisticEmpNo = logisticEmpNo;
        this.startDate = startDate;
        this.productionCost = productionCost;
        this.supplyPrice = supplyPrice;
        this.salesPrice = salesPrice;
        this.isActive = isActive;
        this.colorCodes = colorCodes;
        this.sizeCodes = sizeCodes;
        this.colorRules = colorRules;
        this.sizeRules = sizeRules;
    }

    public Long getStylesId() {
        return stylesId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDesignerEmpNo() {
        return designerEmpNo;
    }

    public String getProductEmpNo() {
        return productEmpNo;
    }

    public String getSalesEmpNo() {
        return salesEmpNo;
    }

    public String getLogisticEmpNo() {
        return logisticEmpNo;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public BigDecimal getProductionCost() {
        return productionCost;
    }

    public BigDecimal getSupplyPrice() {
        return supplyPrice;
    }

    public BigDecimal getSalesPrice() {
        return salesPrice;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public List<String> getColorCodes() {
        return colorCodes;
    }

    public List<String> getSizeCodes() {
        return sizeCodes;
    }

    public List<StyleRuleCodeView> getColorRules() {
        return colorRules;
    }

    public List<StyleRuleCodeView> getSizeRules() {
        return sizeRules;
    }
}
