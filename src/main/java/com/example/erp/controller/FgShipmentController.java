package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.FgShipmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fg/shipments")
public class FgShipmentController extends PageViewSupport {

    private final FgShipmentService fgShipmentService;

    public FgShipmentController(FgShipmentService fgShipmentService) {
        this.fgShipmentService = fgShipmentService;
    }

    @GetMapping
    public String list(Model model) {
    	populate(model, "완제품 출고", "shipment", "pages/fg-shipments", fgShipmentService.findAll());
        return "layout/layout";
    }
}