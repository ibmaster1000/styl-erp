package com.example.erp.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @Column(name = "work_order_id", nullable = false, length = 50)
    private String workOrderId;

    @Column(name = "style_code", length = 50)
    private String styleCode;

    @Column(name = "agreement_code", length = 50)
    private String agreementCode;

    @Column(name = "color_code", length = 50)
    private String colorCode;

    @Column(name = "size_code", length = 50)
    private String sizeCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", length = 30)
    private String status;

    public String getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(String workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getAgreementCode() {
        return agreementCode;
    }

    public void setAgreementCode(String agreementCode) {
        this.agreementCode = agreementCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
