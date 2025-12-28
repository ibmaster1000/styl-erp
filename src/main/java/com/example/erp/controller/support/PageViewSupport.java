package com.example.erp.controller.support;

import com.example.erp.controller.dto.KpiSummary;
import org.springframework.ui.Model;

public class PageViewSupport {

    protected void populate(Model model, String title, String activeMenu, String content, Object list) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("content", content);
        model.addAttribute("list", list);
        model.addAttribute("kpi", KpiSummary.sample());
    }
}