package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.ProductionAgreementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/production-agreements")
public class ProductionAgreementController extends PageViewSupport {

    private final ProductionAgreementService productionAgreementService;

    public ProductionAgreementController(ProductionAgreementService productionAgreementService) {
        this.productionAgreementService = productionAgreementService;
    }

    @GetMapping
    public String list(Model model) {
        populate(model, "생산 합의", "pa", "pages/production-agreements :: content", productionAgreementService.findAll());
        return "layout/layout";
    }
}