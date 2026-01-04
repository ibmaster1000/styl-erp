package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.service.ProductionAgreementService;
import com.example.erp.service.StyleRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/production-agreements")
public class ProductionAgreementController extends PageViewSupport {

    private final ProductionAgreementService productionAgreementService;
    private final StyleRuleService styleRuleService;
    public ProductionAgreementController(ProductionAgreementService productionAgreementService,
            StyleRuleService styleRuleService) {
this.productionAgreementService = productionAgreementService;
this.styleRuleService = styleRuleService;
}

@GetMapping
public String list(Model model) {
	List<ProductionAgreement> agreements = productionAgreementService.findAll();
    Set<String> styleNos = productionAgreementService.extractStyleNos(agreements);
    Map<String, BigDecimal> supplyPrices = styleRuleService.findSupplyPrices(styleNos);
    Map<String, Map<String, List<String>>> styleRules = styleRuleService.findRulesByStyleNos(styleNos);

    populate(model, "생산 합의", "pa", "pages/production-agreements", agreements);
    model.addAttribute("agreementDetails",
            productionAgreementService.buildDetailViews(agreements, styleRules, supplyPrices));
    model.addAttribute("styleRules", styleRules);
    model.addAttribute("styleSupplyPrices", supplyPrices);
    return "layout/layout";
}
}