package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialTransactionSaveLine {
    private Long bomId;
    private String mOrderCode;
    private BigDecimal quantity;
    private BigDecimal plannedOutQuantity;
    private BigDecimal issuedOutQuantity;
    private String orderUom;
    private BigDecimal unitPrice;
    private String factoryCode;
    private String remark;

    public Long getBomId() {
        return bomId;
    }

    public void setBomId(Long bomId) {
        this.bomId = bomId;
    }

    public String getMOrderCode() {
        return mOrderCode;
    }

    public void setMOrderCode(String mOrderCode) {
        this.mOrderCode = mOrderCode;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPlannedOutQuantity() {
        return plannedOutQuantity;
    }

    public void setPlannedOutQuantity(BigDecimal plannedOutQuantity) {
        this.plannedOutQuantity = plannedOutQuantity;
    }

    public BigDecimal getIssuedOutQuantity() {
        return issuedOutQuantity;
    }

    public void setIssuedOutQuantity(BigDecimal issuedOutQuantity) {
        this.issuedOutQuantity = issuedOutQuantity;
    }

    public String getOrderUom() {
        return orderUom;
    }

    public void setOrderUom(String orderUom) {
        this.orderUom = orderUom;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getFactoryCode() {
        return factoryCode;
    }

    public void setFactoryCode(String factoryCode) {
        this.factoryCode = factoryCode;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
