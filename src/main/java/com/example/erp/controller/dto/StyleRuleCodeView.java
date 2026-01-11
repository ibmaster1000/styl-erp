package com.example.erp.controller.dto;

public class StyleRuleCodeView {

    private final String code;
    private final String codeName;

    public StyleRuleCodeView(String code, String codeName) {
        this.code = code;
        this.codeName = codeName;
    }

    public String getCode() {
        return code;
    }

    public String getCodeName() {
        return codeName;
    }
}
