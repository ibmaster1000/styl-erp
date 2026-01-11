package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.controller.dto.AgreementLeftRow;
import com.example.erp.repository.ProductionAgreementView;
import com.example.erp.service.ProductionAgreementService;
import com.example.erp.service.StyleRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
	public String list(@RequestParam(name = "styleCode", required = false) String styleCode,
			@RequestParam(name = "agreementCode", required = false) String agreementCode, Model model) {
		List<ProductionAgreementView> agreements = productionAgreementService.findAll(styleCode, agreementCode);
		Set<String> styleCodes = productionAgreementService.extractStyleCodes(agreements);
		Map<String, BigDecimal> supplyPrices = styleRuleService.findSupplyPrices(styleCodes);
		Map<String, Map<String, List<String>>> styleRules = styleRuleService.findRulesByStyleCodes(styleCodes);
		List<AgreementLeftRow> leftRows = productionAgreementService.buildLeftRows(agreements);

		populate(model, "생산 합의", "pa", "pages/production-agreements", leftRows);
		model.addAttribute("agreementDetails",
				productionAgreementService.buildDetailViewsByColor(agreements, styleRules, supplyPrices));
		model.addAttribute("styleRules", styleRules);
		model.addAttribute("styleSupplyPrices", supplyPrices);
		return "layout/layout";
	}
}
