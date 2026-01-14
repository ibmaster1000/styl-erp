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
	public List<MaterialTransactionLineView> fetchList(@RequestParam(name = "stylesId", required = false) String stylesId,
			@RequestParam(name = "styleCode", required = false) String styleCode,
			@RequestParam(name = "prdAgreeCode", required = false) String prdAgreeCode,
			@RequestParam(name = "colorCode", required = false) String colorCode) {
		return materialTransactionService.findMaterials(stylesId, styleCode, prdAgreeCode, colorCode);
	}

	@GetMapping("/tx-list")
	@ResponseBody
	public List<MaterialTransactionLineView> fetchTransactions(@RequestParam(name = "styleCode", required = false) String styleCode,
			@RequestParam(name = "tranDate", required = false) String tranDate) {
		return materialTransactionService.findTransactionsByType("OUT", styleCode, tranDate);
	}

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveOutbound(@RequestBody MaterialTransactionSaveRequest request,
            Principal principal) {
        String empNo = resolveEmpNo(principal);
        Map<String, Object> result = materialTransactionService.saveOutbound(request, empNo);
        return ResponseEntity.ok(result);
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
