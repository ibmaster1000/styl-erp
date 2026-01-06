package com.example.erp.controller.dto;

public class WorkOrderDetailSaveRequest {
    private String styleNo;
    private String agreementCode;
    private String color;
    private String factoryCode;
    private String factoryName;
    private String dueDate;
    private String deliveryPlaceCode;
    private String deliveryPlaceName;

    public String getStyleNo() {
        return styleNo;
    }

    public void setStyleNo(String styleNo) {
        this.styleNo = styleNo;
    }

    public String getAgreementCode() {
        return agreementCode;
    }

    public void setAgreementCode(String agreementCode) {
        this.agreementCode = agreementCode;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getFactoryCode() {
        return factoryCode;
    }

    public void setFactoryCode(String factoryCode) {
        this.factoryCode = factoryCode;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
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
}
