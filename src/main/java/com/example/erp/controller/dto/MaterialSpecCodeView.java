package com.example.erp.controller.dto;

public class MaterialSpecCodeView {
    private final String code;
    private final String codeName;
    private final String remark;

    public MaterialSpecCodeView(String code, String codeName, String remark) {
        this.code = code;
        this.codeName = codeName;
        this.remark = remark;
    }

    public String getCode() {
        return code;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getRemark() {
        return remark;
    }
}
