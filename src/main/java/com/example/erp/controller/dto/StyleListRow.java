package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StyleListRow {

    private final String styleNo;
    private final String item;
    private final String colors;
    private final String sizes;
    private final String designer;
    private final String productionManager;
    private final String salesManager;
    private final String transportManager;
    private final BigDecimal amount;
    private final LocalDate startDate;
    private final boolean active;

    public StyleListRow(String styleNo,
                        String item,
                        String colors,
                        String sizes,
                        String designer,
                        String productionManager,
                        String salesManager,
                        String transportManager,
                        BigDecimal amount,
                        LocalDate startDate,
                        boolean active) {
        this.styleNo = styleNo;
        this.item = item;
        this.colors = colors;
        this.sizes = sizes;
        this.designer = designer;
        this.productionManager = productionManager;
        this.salesManager = salesManager;
        this.transportManager = transportManager;
        this.amount = amount;
        this.startDate = startDate;
        this.active = active;
    }

    public String getStyleNo() {
        return styleNo;
    }

    public String getItem() {
        return item;
    }

    public String getColors() {
        return colors;
    }

    public String getSizes() {
        return sizes;
    }

    public String getDesigner() {
        return designer;
    }

    public String getProductionManager() {
        return productionManager;
    }

    public String getSalesManager() {
        return salesManager;
    }

    public String getTransportManager() {
        return transportManager;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public boolean isActive() {
        return active;
    }
}