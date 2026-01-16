package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class WorkOrderAgreementRow {
    private String styleCode;
    private String prdAgreeCode;
    private String colorCode;
    private BigDecimal agreementQuantity;
    private String producerCode;
    private String producerName;
    private String dueDate;
    private String deliveryPlaceCode;
    private String deliveryPlaceName;
    private Long prdAgreeId;
    private String status;
    private boolean ordered;

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

    public String getProducerName() {
        return producerName;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
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

    public String getDeliveryPlaceName() {
        return deliveryPlaceName;
    }

    public void setDeliveryPlaceName(String deliveryPlaceName) {
        this.deliveryPlaceName = deliveryPlaceName;
    }

    public Long getPrdAgreeId() {
        return prdAgreeId;
    }

    public void setPrdAgreeId(Long prdAgreeId) {
        this.prdAgreeId = prdAgreeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }
}
