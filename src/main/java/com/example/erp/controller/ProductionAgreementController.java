package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.ProductionAgreementService;
import com.example.erp.service.ProductionAgreementService.AgreementPageResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;


@Controller
@RequestMapping("/production-agreements")
public class ProductionAgreementController extends PageViewSupport {

	private final ProductionAgreementService productionAgreementService;

	public ProductionAgreementController(ProductionAgreementService productionAgreementService) {
		this.productionAgreementService = productionAgreementService;
	}

	@GetMapping
	public String list(@RequestParam(name = "styleCode", required = false) String styleCode,
			@RequestParam(name = "agreementCode", required = false) String agreementCode,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "selectedAgreementCode", required = false) String selectedAgreementCode,
			Model model) {
		AgreementPageResult pageResult = productionAgreementService.fetchAgreementPage(styleCode, agreementCode, page,
				25, selectedAgreementCode);

		populate(model, "생산 합의", "pa", "pages/production-agreements", pageResult.leftRows());
		model.addAttribute("pageInfo", pageResult.pageInfo());
		model.addAttribute("selectedAgreementCode", pageResult.selectedAgreementCode());
		model.addAttribute("detailView", pageResult.detailView());
		model.addAttribute("styleCode", styleCode);
		model.addAttribute("agreementCode", agreementCode);
		return "layout/layout";
	}

	@PatchMapping("/{agreementCode}/complete")
	public ResponseEntity<Void> updateCompletion(@PathVariable("agreementCode") String agreementCode,
			@RequestParam(name = "colorCode", required = false) String colorCode,
			@RequestParam(name = "value") boolean completed) {
		boolean updated = productionAgreementService.updateCompletionStatus(agreementCode, colorCode, completed);
		if (!updated) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().build();
	}
}
