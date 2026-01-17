package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialOrderSelection;
import com.example.erp.controller.dto.MaterialOrderStyleResult;
import com.example.erp.controller.dto.MaterialSpecCodeView;
import com.example.erp.controller.dto.WorkOrderAgreementRow;
import com.example.erp.controller.dto.WorkOrderSaveRequest;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.Code;
import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import com.example.erp.service.CodeService;
import com.example.erp.service.MaterialOrderService;
import com.example.erp.service.ProductionWorkOrderService;
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
@RequestMapping("/production/work-orders")
public class ProductionWorkOrderController extends PageViewSupport {
	
    private static final Logger log = LoggerFactory.getLogger(ProductionWorkOrderController.class);
	private final MaterialOrderService materialOrderService;
    private final ProductionWorkOrderService productionWorkOrderService;
    private final UserRepository userRepository;
    private final CodeService codeService;

    public ProductionWorkOrderController(MaterialOrderService materialOrderService,
            ProductionWorkOrderService productionWorkOrderService,
            UserRepository userRepository,
            CodeService codeService) {
        this.materialOrderService = materialOrderService;
        this.productionWorkOrderService = productionWorkOrderService;
        this.userRepository = userRepository;
        this.codeService = codeService;
    }

    @GetMapping
    public String view(Model model) {
    	Map<String, Object> headerPage = productionWorkOrderService.findAgreementHeaderPage(null, null, 1, 25);
        Object items = headerPage.getOrDefault("items", Collections.emptyList());
        populate(model, "작업지시", "work-instructions", "production/work-orders", items);
        model.addAttribute("headerPage", headerPage);
        model.addAttribute("page", headerPage.get("page"));
        model.addAttribute("size", headerPage.get("size"));
        model.addAttribute("total", headerPage.get("total"));
        model.addAttribute("totalPages", headerPage.get("totalPages"));
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
    public ResponseEntity<?> fetchList(@RequestParam(name = "styleCode", required = false) String styleCode,
            @RequestParam(name = "prdAgreeCode", required = false) String prdAgreeCode,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "25") int size) {
        try {
        	Map<String, Object> result =
                    productionWorkOrderService.findAgreementHeaderPage(styleCode, prdAgreeCode, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("작업지시 목록 조회 중 오류가 발생했습니다.", e);
            String message = "조회 중 오류가 발생했습니다. (원인: " + e.getMessage() + ")";
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }
    
    @GetMapping("/details")
    @ResponseBody
    public ResponseEntity<?> fetchDetails(@RequestParam(name = "stylesId", required = false) String stylesId,
            @RequestParam(name = "styleCode", required = false) String styleCode,
            @RequestParam(name = "prdAgreeCode", required = false) String prdAgreeCode) {
        try {
            List<WorkOrderAgreementRow> result =
                    productionWorkOrderService.findAgreementRowsByAgreement(stylesId, styleCode, prdAgreeCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("작업지시 상세 조회 중 오류가 발생했습니다.", e);
            String message = "조회 중 오류가 발생했습니다. (원인: " + e.getMessage() + ")";
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }

    @GetMapping("/codes")
    @ResponseBody
    public List<MaterialSpecCodeView> loadCodes(@RequestParam("codeType") String codeType,
            @RequestParam(name = "keyword", required = false) String keyword) {
        List<Code> codes = codeService.searchActiveCodes(codeType, null, null, null);
        if (!StringUtils.hasText(keyword)) {
            return codes.stream()
                    .map(code -> new MaterialSpecCodeView(code.getCode(), code.getCodeName(), code.getDescription()))
                    .toList();
        }
        String normalized = keyword.trim().toLowerCase();
        return codes.stream()
                .filter(code -> containsKeyword(code.getCode(), normalized)
                        || containsKeyword(code.getCodeName(), normalized)
                        || containsKeyword(code.getDescription(), normalized))
                .map(code -> new MaterialSpecCodeView(code.getCode(), code.getCodeName(), code.getDescription()))
                .toList();
    }

    @PostMapping("/draft")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody WorkOrderSaveRequest request,
            Principal principal) {
        String empNo = resolveEmpNo(principal);
        try {
            Map<String, Object> result = productionWorkOrderService.saveDraftWorkOrder(request, empNo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("작업지시 저장 중 오류가 발생했습니다.", e);
            String message = "작업지시 저장에 실패했습니다. (원인: " + e.getMessage() + ")";
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }

    @PostMapping("/activate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> activate(@RequestBody WorkOrderSaveRequest request,
            Principal principal) {
        String empNo = resolveEmpNo(principal);
        try {
            Map<String, Object> result = productionWorkOrderService.activateWorkOrder(request, empNo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("작업지시 활성화 중 오류가 발생했습니다.", e);
            String message = "작업지시 활성화에 실패했습니다. (원인: " + e.getMessage() + ")";
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(@RequestBody WorkOrderSaveRequest request,
            Principal principal) {
        String empNo = resolveEmpNo(principal);
        try {
            Map<String, Object> result = productionWorkOrderService.saveDraftWorkOrder(request, empNo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("작업지시 저장 중 오류가 발생했습니다.", e);
            String message = "작업지시 저장에 실패했습니다. (원인: " + e.getMessage() + ")";
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

    private boolean containsKeyword(String value, String keyword) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.toLowerCase().contains(keyword);
    }
}
