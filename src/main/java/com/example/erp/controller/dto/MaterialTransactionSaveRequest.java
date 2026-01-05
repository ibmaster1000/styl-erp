package com.example.erp.controller.dto;

import java.util.List;

public class MaterialTransactionSaveRequest {
    private String stylesId;
    private String styleCode;
    private String prdAgreeCode;
    private String colorCode;
    private String tranDate;
    private List<MaterialTransactionSaveLine> items;

    public String getStylesId() {
        return stylesId;
    }

    public void setStylesId(String stylesId) {
        this.stylesId = stylesId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getPrdAgreeCode() {
        return prdAgreeCode;
    }

    public void setPrdAgreeCode(String prdAgreeCode) {
        this.prdAgreeCode = prdAgreeCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getTranDate() {
        return tranDate;
    }

    public void setTranDate(String tranDate) {
        this.tranDate = tranDate;
    }

    public List<MaterialTransactionSaveLine> getItems() {
        return items;
    }

    public void setItems(List<MaterialTransactionSaveLine> items) {
        this.items = items;
    }
}