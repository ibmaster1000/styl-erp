package com.example.erp.controller.dto;

import java.util.List;

public class WorkOrderDetailSavePayload {
    private List<WorkOrderDetailSaveRequest> items;

    public List<WorkOrderDetailSaveRequest> getItems() {
        return items;
    }

    public void setItems(List<WorkOrderDetailSaveRequest> items) {
        this.items = items;
    }
}
