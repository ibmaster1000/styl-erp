package com.example.erp.controller.dto;

public class CodeSearchResult {

    private final String codeType;
    private final String code;
    private final String codeName;

    public CodeSearchResult(String codeType, String code, String codeName) {
        this.codeType = codeType;
        this.code = code;
        this.codeName = codeName;
    }

    public String getCodeType() {
        return codeType;
    }

    public String getCode() {
        return code;
    }

    public String getCodeName() {
        return codeName;
    }
}
