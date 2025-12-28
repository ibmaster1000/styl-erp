package com.example.erp.controller.dto;

public record KpiSummary(int workOrdersInProgress, int materialShortage, int todayReceipts, int todayShipments) {
    public static KpiSummary sample() {
        return new KpiSummary(12, 3, 8, 5);
    }
}