package com.example.erp.controller.dto;

public class SimpleCodeView {
    private final String code;
    private final String name;

    public SimpleCodeView(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}