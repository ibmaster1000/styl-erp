package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/production/work-orders")
public class ProductionWorkOrderController extends PageViewSupport {

    @GetMapping
    public String view(Model model) {
        populate(model, "작업지시", "work-instructions", "production/work-orders", Collections.emptyList());
        return "layout/layout";
    }
}