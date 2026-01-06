package com.example.erp.controller.dto;

import java.time.LocalDate;

public class WorkOrderAgreementDetail {

    private String styleNo;
    private String agreementCode;
    private String agreementLabel;
    private String color;
    private Integer quantity;
    private String productionSite;
    private String factoryCode;
    private String factoryName;
    private LocalDate dueDate;
    private String deliveryPlaceCode;
    private String deliveryPlaceName;

    public WorkOrderAgreementDetail() {
    }

    public WorkOrderAgreementDetail(String styleNo, String agreementCode, String agreementLabel, String color,
            Integer quantity, String productionSite, String factoryCode, String factoryName, LocalDate dueDate,
            String deliveryPlaceCode, String deliveryPlaceName) {
        this.styleNo = styleNo;
        this.agreementCode = agreementCode;
        this.agreementLabel = agreementLabel;
        this.color = color;
        this.quantity = quantity;
        this.productionSite = productionSite;
        this.factoryCode = factoryCode;
        this.factoryName = factoryName;
        this.dueDate = dueDate;
        this.deliveryPlaceCode = deliveryPlaceCode;
        this.deliveryPlaceName = deliveryPlaceName;
    }

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

    public String getAgreementLabel() {
        return agreementLabel;
    }

    public void setAgreementLabel(String agreementLabel) {
        this.agreementLabel = agreementLabel;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getProductionSite() {
        return productionSite;
    }

    public void setProductionSite(String productionSite) {
        this.productionSite = productionSite;
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
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
