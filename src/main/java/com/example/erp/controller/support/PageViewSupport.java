package com.example.erp.controller.support;

import com.example.erp.controller.dto.KpiSummary;
import org.springframework.core.io.ClassPathResource;
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
        	return "fragments/empty-content";
        }
        String trimmed = content.trim();
        int fragmentIndex = trimmed.indexOf("::");
        if (fragmentIndex > -1) {
        	trimmed = trimmed.substring(0, fragmentIndex).trim();
        }
        if (!templateExists(trimmed)) {
            return "fragments/empty-content";
        }
        return trimmed;
    }
    private boolean templateExists(String contentPath) {
        if (contentPath == null || contentPath.isBlank()) {
            return false;
        }
        String normalizedPath = contentPath.startsWith("/") ? contentPath.substring(1) : contentPath;
        ClassPathResource resource = new ClassPathResource("templates/" + normalizedPath + ".html");
        return resource.exists();
    }
}