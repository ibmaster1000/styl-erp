package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class WorkOrderSaveRequest {
    private String styleCode;
    private String prdAgreeCode;
    private String colorCode;
    private BigDecimal agreementQuantity;
    private String producerCode;
    private String dueDate;
    private String deliveryPlaceCode;

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getPrdAgreeCode() {
        return prdAgreeCode;
    }

    public void setPrdAgreeCode(String prdAgreeCode) {
        this.prdAgreeCode = prdAgreeCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public BigDecimal getAgreementQuantity() {
        return agreementQuantity;
    }

    public void setAgreementQuantity(BigDecimal agreementQuantity) {
        this.agreementQuantity = agreementQuantity;
    }

    public String getProducerCode() {
        return producerCode;
    }

    public void setProducerCode(String producerCode) {
        this.producerCode = producerCode;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDeliveryPlaceCode() {
        return deliveryPlaceCode;
    }

    public void setDeliveryPlaceCode(String deliveryPlaceCode) {
        this.deliveryPlaceCode = deliveryPlaceCode;
    }
}