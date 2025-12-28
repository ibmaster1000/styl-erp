package com.example.erp.controller.support;

import com.example.erp.controller.dto.KpiSummary;
import org.springframework.ui.Model;

public class PageViewSupport {

    protected void populate(Model model, String title, String activeMenu, String content, Object list) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("content", normalizeContent(content));
        model.addAttribute("list", list);
        model.addAttribute("kpi", KpiSummary.sample());
    }
    
    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        return content.trim();
    }
}