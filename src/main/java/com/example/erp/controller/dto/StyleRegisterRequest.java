package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StyleRegisterRequest {

    private String itemCode;
    private LocalDate startDate;
    private BigDecimal costPrice;
    private BigDecimal productionCost;
    private BigDecimal supplyPrice;
    private BigDecimal salesPrice;
    private String designerEmpNo;
    private String productEmpNo;
    private String salesEmpNo;
    private String logisticEmpNo;
    private List<String> colorCodes;
    private List<String> sizeCodes;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
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

    public String getDesignerEmpNo() {
        return designerEmpNo;
    }

    public void setDesignerEmpNo(String designerEmpNo) {
        this.designerEmpNo = designerEmpNo;
    }

    public String getProductEmpNo() {
        return productEmpNo;
    }

    public void setProductEmpNo(String productEmpNo) {
        this.productEmpNo = productEmpNo;
    }

    public String getSalesEmpNo() {
        return salesEmpNo;
    }

    public void setSalesEmpNo(String salesEmpNo) {
        this.salesEmpNo = salesEmpNo;
    }

    public String getLogisticEmpNo() {
        return logisticEmpNo;
    }

    public void setLogisticEmpNo(String logisticEmpNo) {
        this.logisticEmpNo = logisticEmpNo;
    }

    public List<String> getColorCodes() {
        return colorCodes;
    }

    public void setColorCodes(List<String> colorCodes) {
        this.colorCodes = colorCodes;
    }

    public List<String> getSizeCodes() {
        return sizeCodes;
    }

    public void setSizeCodes(List<String> sizeCodes) {
        this.sizeCodes = sizeCodes;
    }
}