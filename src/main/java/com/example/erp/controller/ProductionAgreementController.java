package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.ProductionAgreementService;
import com.example.erp.service.ProductionAgreementService.AgreementPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller
@RequestMapping("/production-agreements")
public class ProductionAgreementController extends PageViewSupport {

	private static final Logger logger = LoggerFactory.getLogger(ProductionAgreementController.class);
	private final ProductionAgreementService productionAgreementService;

	public ProductionAgreementController(ProductionAgreementService productionAgreementService) {
		this.productionAgreementService = productionAgreementService;
	}

	@GetMapping
	public String list(@RequestParam(name = "styleCode", required = false) String styleCode,
			@RequestParam(name = "agreementCode", required = false) String agreementCode,
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "selectedAgreementCode", required = false) String selectedAgreementCode,
			@RequestParam(name = "selectedColorCode", required = false) String selectedColorCode,
			Model model, @ModelAttribute("message") String message) {
		AgreementPageResult pageResult = productionAgreementService.fetchAgreementPage(styleCode, agreementCode, page,
				25, selectedAgreementCode, selectedColorCode);

		populate(model, "생산 합의", "pa", "pages/production-agreements", pageResult.leftRows());
		model.addAttribute("pageInfo", pageResult.pageInfo());
		model.addAttribute("selectedAgreementCode", pageResult.selectedAgreementCode());
		model.addAttribute("selectedColorCode", pageResult.selectedColorCode());
		model.addAttribute("detailView", pageResult.detailView());
		model.addAttribute("styleCode", styleCode);
		model.addAttribute("agreementCode", agreementCode);
		model.addAttribute("message", message);
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

	@PostMapping("/create")
	public String create(@ModelAttribute("form") ProductionAgreementForm form,
			RedirectAttributes redirectAttributes) {
		logger.info("Production agreement create request: agreementCode={}, styleCode={}, colorCode={}, sizeCode={}",
				form.getAgreementCode(), form.getStyleCode(), form.getColorCode(), form.getSizeCode());
		try {
			productionAgreementService.createAgreement(form.getAgreementCode(), form.getStyleCode(),
					form.getColorType(), form.getColorCode(), form.getSizeType(), form.getSizeCode(),
					form.getQuantity(), form.getProductionManager());
			redirectAttributes.addFlashAttribute("message", "생산합의가 등록되었습니다.");
		} catch (RuntimeException e) {
			logger.info("Production agreement create failed: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}
		return "redirect:/production-agreements";
	}

	@PostMapping("/update")
	public String update(@RequestParam("agreementCode") String agreementCode,
			@RequestParam("colorCode") List<String> colorCodes,
			@RequestParam("sizeCode") List<String> sizeCodes,
			@RequestParam("quantity") List<Integer> quantities,
			RedirectAttributes redirectAttributes) {
		logger.info("Production agreement update request: agreementCode={}", agreementCode);
		try {
			int updated = productionAgreementService.updateAgreementQuantities(agreementCode, colorCodes, sizeCodes,
					quantities);
			redirectAttributes.addFlashAttribute("message",
					updated > 0 ? "생산합의가 수정되었습니다." : "수정할 생산합의가 없습니다.");
		} catch (RuntimeException e) {
			logger.info("Production agreement update failed: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}
		redirectAttributes.addAttribute("selectedAgreementCode", agreementCode);
		return "redirect:/production-agreements";
	}

	@PostMapping("/delete")
	public String delete(@RequestParam("agreementCode") String agreementCode,
			@RequestParam(name = "colorCode", required = false) String colorCode,
			RedirectAttributes redirectAttributes) {
		logger.info("Production agreement delete request: agreementCode={}, colorCode={}", agreementCode, colorCode);
		try {
			int deleted = productionAgreementService.deleteAgreement(agreementCode, colorCode);
			redirectAttributes.addFlashAttribute("message",
					deleted > 0 ? "생산합의가 삭제되었습니다." : "삭제할 생산합의를 찾을 수 없습니다.");
		} catch (RuntimeException e) {
			logger.info("Production agreement delete failed: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}
		return "redirect:/production-agreements";
	}

	public static class ProductionAgreementForm {
		private String agreementCode;
		private String styleCode;
		private String colorType;
		private String colorCode;
		private String sizeType;
		private String sizeCode;
		private Integer quantity;
		private String productionManager;

		public String getAgreementCode() {
			return agreementCode;
		}

		public void setAgreementCode(String agreementCode) {
			this.agreementCode = agreementCode;
		}

		public String getStyleCode() {
			return styleCode;
		}

		public void setStyleCode(String styleCode) {
			this.styleCode = styleCode;
		}

		public String getColorType() {
			return colorType;
		}

		public void setColorType(String colorType) {
			this.colorType = colorType;
		}

		public String getColorCode() {
			return colorCode;
		}

		public void setColorCode(String colorCode) {
			this.colorCode = colorCode;
		}

		public String getSizeType() {
			return sizeType;
		}

		public void setSizeType(String sizeType) {
			this.sizeType = sizeType;
		}

		public String getSizeCode() {
			return sizeCode;
		}

		public void setSizeCode(String sizeCode) {
			this.sizeCode = sizeCode;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}

		public String getProductionManager() {
			return productionManager;
		}

		public void setProductionManager(String productionManager) {
			this.productionManager = productionManager;
		}
	}
}
