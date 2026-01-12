package com.example.erp.controller.dto;

import java.util.ArrayList;
import java.util.List;

public class MaterialSpecSaveRequest {
    private String styleCode;
    private String colorCode;
    private String prdAgreeCode;
    private List<MaterialSpecSaveItem> items = new ArrayList<>();
    private List<Long> deletedIds = new ArrayList<>();

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getPrdAgreeCode() {
        return prdAgreeCode;
    }

    public void setPrdAgreeCode(String prdAgreeCode) {
        this.prdAgreeCode = prdAgreeCode;
    }

    public List<MaterialSpecSaveItem> getItems() {
        return items;
    }

    public void setItems(List<MaterialSpecSaveItem> items) {
        this.items = items;
    }

    public List<Long> getDeletedIds() {
        return deletedIds;
    }

    public void setDeletedIds(List<Long> deletedIds) {
        this.deletedIds = deletedIds;
    }
}
