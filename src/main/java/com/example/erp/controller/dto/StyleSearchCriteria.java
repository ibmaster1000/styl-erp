package com.example.erp.controller.dto;

import org.springframework.format.annotation.NumberFormat;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class StyleSearchCriteria {

	private String styleCode;
    private String designer;
    private Boolean active;

    @NumberFormat
    private BigDecimal productionCostMin;
    @NumberFormat
    private BigDecimal productionCostMax;
    @NumberFormat
    private BigDecimal supplyPriceMin;
    @NumberFormat
    private BigDecimal supplyPriceMax;
    @NumberFormat
    private BigDecimal salesPriceMin;
    @NumberFormat
    private BigDecimal salesPriceMax;

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getDesigner() {
        return designer;
    }

    public void setDesigner(String designer) {
        this.designer = designer;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BigDecimal getProductionCostMin() {
        return productionCostMin;
    }

    public void setProductionCostMin(BigDecimal productionCostMin) {
        this.productionCostMin = productionCostMin;
    }

    public BigDecimal getProductionCostMax() {
        return productionCostMax;
    }

    public void setProductionCostMax(BigDecimal productionCostMax) {
        this.productionCostMax = productionCostMax;
    }

    public BigDecimal getSupplyPriceMin() {
        return supplyPriceMin;
    }

    public void setSupplyPriceMin(BigDecimal supplyPriceMin) {
        this.supplyPriceMin = supplyPriceMin;
    }

    public BigDecimal getSupplyPriceMax() {
        return supplyPriceMax;
    }

    public void setSupplyPriceMax(BigDecimal supplyPriceMax) {
        this.supplyPriceMax = supplyPriceMax;
    }

    public BigDecimal getSalesPriceMin() {
        return salesPriceMin;
    }

    public void setSalesPriceMin(BigDecimal salesPriceMin) {
        this.salesPriceMin = salesPriceMin;
    }

    public BigDecimal getSalesPriceMax() {
        return salesPriceMax;
    }

    public void setSalesPriceMax(BigDecimal salesPriceMax) {
        this.salesPriceMax = salesPriceMax;
    }

    public boolean hasFilters() {
    	return StringUtils.hasText(styleCode) ||
                StringUtils.hasText(designer) ||
                active != null ||
                productionCostMin != null || productionCostMax != null ||
                supplyPriceMin != null || supplyPriceMax != null ||
                salesPriceMin != null || salesPriceMax != null;
    }
}