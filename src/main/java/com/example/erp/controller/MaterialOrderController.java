package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialOrderItemView;
import com.example.erp.controller.dto.MaterialOrderRequest;
import com.example.erp.controller.dto.MaterialOrderSelection;
import com.example.erp.controller.dto.MaterialOrderStyleResult;
import com.example.erp.controller.dto.MaterialOrderSupplierView;
import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import com.example.erp.service.MaterialOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/production/material-orders")
public class MaterialOrderController {

    private final MaterialOrderService materialOrderService;
    private final UserRepository userRepository;

    public MaterialOrderController(MaterialOrderService materialOrderService, UserRepository userRepository) {
        this.materialOrderService = materialOrderService;
        this.userRepository = userRepository;
    }

    @GetMapping("/styles")
    @ResponseBody
    public List<MaterialOrderStyleResult> searchStyles(@RequestParam(required = false) String keyword) {
        return materialOrderService.searchStyles(keyword);
    }

    @GetMapping("/options")
    @ResponseBody
    public MaterialOrderSelection loadSelection(@RequestParam(required = false) String stylesId,
            @RequestParam(required = false) String styleCode) {
        return materialOrderService.loadSelection(stylesId, styleCode);
    }

    @GetMapping("/suppliers")
    @ResponseBody
    public List<MaterialOrderSupplierView> fetchSuppliers(@RequestParam(required = false) String stylesId,
            @RequestParam String prdAgreeCode,
            @RequestParam String colorCode) {
        return materialOrderService.findSuppliers(stylesId, prdAgreeCode, colorCode);
    }

    @GetMapping("/materials")
    @ResponseBody
    public List<MaterialOrderItemView> fetchMaterials(@RequestParam(required = false) String stylesId,
            @RequestParam String prdAgreeCode,
            @RequestParam String colorCode,
            @RequestParam String supplierCode) {
        return materialOrderService.findMaterials(stylesId, prdAgreeCode, colorCode, supplierCode);
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitOrder(@RequestBody MaterialOrderRequest request,
            Principal principal) {
        String orderedBy = resolveEmpNo(principal);
        Map<String, Object> result = materialOrderService.submitOrders(request, orderedBy);
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