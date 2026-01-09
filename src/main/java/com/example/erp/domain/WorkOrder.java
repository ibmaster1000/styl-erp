package com.example.erp.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "work_orders")
public class WorkOrder extends BaseAuditEntity {

    @Id
    @Column(name = "work_order_id", nullable = false, length = 50)
    private String workOrderId;

    @Column(name = "style_code", length = 50)
    private String styleCode;

    @Column(length = 50)
    private String agreementCode;

    @Column(length = 30)
    private String color;

    @Column(length = 30)
    private String size;

    private Integer quantity;

    private LocalDate dueDate;

    @Column(length = 30)
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
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