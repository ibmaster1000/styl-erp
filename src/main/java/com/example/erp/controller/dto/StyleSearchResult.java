package com.example.erp.controller.dto;

import java.util.List;
import java.util.Map;

public class StyleSearchResult {

    private final List<StyleListRow> list;
    private final Map<String, Map<String, List<String>>> stylesRule;

    public StyleSearchResult(List<StyleListRow> list,
                             Map<String, Map<String, List<String>>> stylesRule) {
        this.list = list;
        this.stylesRule = stylesRule;
    }

    public List<StyleListRow> getList() {
        return list;
    }

    public Map<String, Map<String, List<String>>> getStylesRule() {
        return stylesRule;
    }
}