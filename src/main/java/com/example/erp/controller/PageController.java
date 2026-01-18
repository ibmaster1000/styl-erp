package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;

@Controller
public class PageController extends PageViewSupport {

    private final DashboardService dashboardService;

    public PageController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
    	model.addAttribute("title", "대시보드");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("content", "dashboard");
        model.addAttribute("kpi", dashboardService.loadKpi());
        model.addAttribute("progress", dashboardService.loadProgress());
        DashboardService.ChartData chartData = dashboardService.loadChartData();
        model.addAttribute("chartLabels", chartData.labels());
        model.addAttribute("chartValues", chartData.values());
        
        return "layout/layout";
    }
    
    @GetMapping("/production/material-orders")
    public String materialOrders(Model model) {
        populate(model, "원부자재 발주", "material-orders", "pages/material-orders", Collections.emptyList());
        return "layout/layout";
    }
}
