package com.example.erp.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "styles")
public class Style extends BaseAuditEntity {

    @Id
    @Column(name = "style_no", nullable = false, length = 50)
    private String styleNo;

    @Column(length = 100)
    private String item;

    @Column(length = 100)
    private String designer;

    @Column(length = 100)
    private String productionManager;

    @Column(length = 100)
    private String salesManager;

    private LocalDate startDate;

    @Column(nullable = false)
    private boolean active = true;

    public String getStyleNo() {
        return styleNo;
    }

    public void setStyleNo(String styleNo) {
        this.styleNo = styleNo;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDesigner() {
        return designer;
    }

    public void setDesigner(String designer) {
        this.designer = designer;
    }

    public String getProductionManager() {
        return productionManager;
    }

    public void setProductionManager(String productionManager) {
        this.productionManager = productionManager;
    }

    public String getSalesManager() {
        return salesManager;
    }

    public void setSalesManager(String salesManager) {
        this.salesManager = salesManager;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}