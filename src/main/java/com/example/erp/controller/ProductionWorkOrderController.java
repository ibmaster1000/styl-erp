package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialOrderSelection;
import com.example.erp.controller.dto.MaterialOrderStyleResult;
import com.example.erp.controller.dto.WorkOrderAgreementRow;
import com.example.erp.controller.dto.WorkOrderSaveRequest;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import com.example.erp.service.MaterialOrderService;
import com.example.erp.service.ProductionWorkOrderService;
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

import java.util.Collections;

@Controller
@RequestMapping("/production/work-orders")
public class ProductionWorkOrderController extends PageViewSupport {
	
	private final MaterialOrderService materialOrderService;
    private final ProductionWorkOrderService productionWorkOrderService;
    private final UserRepository userRepository;

    public ProductionWorkOrderController(MaterialOrderService materialOrderService,
            ProductionWorkOrderService productionWorkOrderService,
            UserRepository userRepository) {
        this.materialOrderService = materialOrderService;
        this.productionWorkOrderService = productionWorkOrderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String view(Model model) {
        populate(model, "작업지시", "work-instructions", "production/work-orders", Collections.emptyList());
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
    public List<WorkOrderAgreementRow> fetchList(@RequestParam(name = "stylesId", required = false) String stylesId,
            @RequestParam(name = "styleCode", required = false) String styleCode,
            @RequestParam(name = "prdAgreeCode", required = false) String prdAgreeCode,
            @RequestParam(name = "colorCode", required = false) String colorCode) {
        return productionWorkOrderService.findAgreementRows(stylesId, styleCode, prdAgreeCode, colorCode);
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody WorkOrderSaveRequest request, Principal principal) {
        String empNo = resolveEmpNo(principal);
        return productionWorkOrderService.saveWorkOrder(request, empNo);
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