package com.example.erp.controller.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StyleForm {

    private String styleCode;
    private String item;
    private String designerName;
    private String designerEmpNo;
    private String productionManager;
    private String salesManager;

    @NumberFormat
    private BigDecimal productionCost;
    @NumberFormat
    private BigDecimal supplyPrice;
    @NumberFormat
    private BigDecimal salesPrice;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    private boolean active = true;

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDesignerName() {
        return designerName;
    }

    public void setDesignerName(String designerName) {
        this.designerName = designerName;
    }

    public String getDesignerEmpNo() {
        return designerEmpNo;
    }

    public void setDesignerEmpNo(String designerEmpNo) {
        this.designerEmpNo = designerEmpNo;
    }

    public String getProductionManager() {
        return productionManager;
    }

    public void setProductionManager(String productionManager) {
        this.productionManager = productionManager;
    }

    public String getSalesManager() {
        return salesManager;
    }

    public void setSalesManager(String salesManager) {
        this.salesManager = salesManager;
    }

    public BigDecimal getProductionCost() {
        return productionCost;
    }

    public void setProductionCost(BigDecimal productionCost) {
        this.productionCost = productionCost;
    }

    public BigDecimal getSupplyPrice() {
        return supplyPrice;
    }

    public void setSupplyPrice(BigDecimal supplyPrice) {
        this.supplyPrice = supplyPrice;
    }

    public BigDecimal getSalesPrice() {
        return salesPrice;
    }

    public void setSalesPrice(BigDecimal salesPrice) {
        this.salesPrice = salesPrice;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDesignerMissing() {
        return !StringUtils.hasText(designerEmpNo) || !StringUtils.hasText(designerName);
    }
}