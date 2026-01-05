package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialTransactionSaveLine {
    private Long bomId;
    private BigDecimal quantity;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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