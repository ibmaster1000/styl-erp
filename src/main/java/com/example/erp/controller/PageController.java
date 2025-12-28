package com.example.erp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
    	model.addAttribute("title", "대시보드");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("content", "dashboard :: content");
        model.addAttribute("kpi", new Kpi(12, 3, 8, 5));
        
        return "layout/layout";
    }

    public record Kpi(int workOrdersInProgress, int materialShortage, int todayReceipts, int todayShipments){}
}
