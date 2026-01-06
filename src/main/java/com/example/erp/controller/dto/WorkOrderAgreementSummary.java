package com.example.erp.controller.dto;

public class WorkOrderAgreementSummary {

    private String styleNo;
    private String agreementCode;
    private String color;
    private Integer quantity;
    private String productionSite;
    private boolean dispatched;

    public WorkOrderAgreementSummary() {
    }

    public WorkOrderAgreementSummary(String styleNo, String agreementCode, String color, Integer quantity,
            String productionSite, boolean dispatched) {
        this.styleNo = styleNo;
        this.agreementCode = agreementCode;
        this.color = color;
        this.quantity = quantity;
        this.productionSite = productionSite;
        this.dispatched = dispatched;
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

    public boolean isDispatched() {
        return dispatched;
    }

    public void setDispatched(boolean dispatched) {
        this.dispatched = dispatched;
    }
}
