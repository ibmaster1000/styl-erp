package com.example.erp.controller.dto;

public class WorkOrderAttachmentView {
    private final String workOrderId;
    private final String type;
    private final String filePath;

    public WorkOrderAttachmentView(String workOrderId, String type, String filePath) {
        this.workOrderId = workOrderId;
        this.type = type;
        this.filePath = filePath;
    }

    public String getWorkOrderId() {
        return workOrderId;
    }

    public String getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }
}