package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialOrderSelection;
import com.example.erp.controller.dto.MaterialOrderStyleResult;
import com.example.erp.controller.dto.MaterialTransactionLineView;
import com.example.erp.controller.dto.MaterialTransactionSaveRequest;
import com.example.erp.controller.dto.SimpleCodeView;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import com.example.erp.service.MaterialOrderService;
import com.example.erp.service.MaterialTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/production/material-outbound")
public class MaterialOutboundController extends PageViewSupport {
	
	private static final Logger log = LoggerFactory.getLogger(MaterialOutboundController.class);
    private final MaterialTransactionService materialTransactionService;
    private final MaterialOrderService materialOrderService;
    private final UserRepository userRepository;

    public MaterialOutboundController(MaterialTransactionService materialTransactionService,
            MaterialOrderService materialOrderService,
            UserRepository userRepository) {
        this.materialTransactionService = materialTransactionService;
        this.materialOrderService = materialOrderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String view(Model model) {
        populate(model, "원부자재 출고 등록", "material-outbound", "production/material-outbound",
                Collections.emptyList());
        List<SimpleCodeView> factories = materialTransactionService.findFactoriesOrCustomers();
        model.addAttribute("factories", factories);
        return "layout/layout";
    }

    @GetMapping("/styles")
    @ResponseBody
    public List<MaterialOrderStyleResult> searchStyles(@RequestParam(name = "keyword", required = false) String keyword) {
        return materialOrderService.searchStyles(keyword);
    }

    @GetMapping("/options")
    @ResponseBody
    public MaterialOrderSelection loadSelection(@RequestParam(name = "stylesId", required = false) String stylesId,
            @RequestParam(name = "styleCode", required = false) String styleCode) {
        return materialOrderService.loadSelection(stylesId, styleCode);
    }

	@GetMapping("/list")
	@ResponseBody
	public ResponseEntity<?> fetchList(@RequestParam(name = "stylesId", required = false) String stylesId,
			@RequestParam(name = "styleCode", required = false) String styleCode,
			@RequestParam(name = "prdAgreeCode", required = false) String prdAgreeCode,
			@RequestParam(name = "colorCode", required = false) String colorCode) {
		try {
			List<MaterialTransactionLineView> result =
					materialTransactionService.findMaterials(stylesId, styleCode, prdAgreeCode, colorCode);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			log.error("원부자재 출고 조회 중 오류가 발생했습니다.", e);
			String message = "조회 중 오류가 발생했습니다. (원인: " + e.getMessage() + ")";
			return ResponseEntity.status(500).body(Map.of("message", message));
		}
	}

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveOutbound(@RequestBody MaterialTransactionSaveRequest request,
            Principal principal) {
        String empNo = resolveEmpNo(principal);
        try {
            Map<String, Object> result = materialTransactionService.saveOutbound(request, empNo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("원부자재 출고 저장 중 오류가 발생했습니다.", e);
            String message = "출고 등록에 실패했습니다. (원인: " + e.getMessage() + ")";
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }

    private String resolveEmpNo(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            return null;
        }
        return userRepository.findByUsername(principal.getName())
                .map(User::getEmpNo)
                .orElse(principal.getName());
    }
}
