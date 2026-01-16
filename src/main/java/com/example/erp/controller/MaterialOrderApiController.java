package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialOrderContextResponse;
import com.example.erp.controller.dto.MaterialOrderRequest;
import com.example.erp.controller.dto.MaterialOrderSearchResponse;
import com.example.erp.controller.dto.MaterialSpecMatrixResponse;
import com.example.erp.controller.dto.MaterialSpecOptionsResponse;
import com.example.erp.service.AgreementQueryService;
import com.example.erp.service.CustomUserPrincipal;
import com.example.erp.service.MaterialOrderService;
import com.example.erp.service.MaterialSpecService;
import com.example.erp.service.StyleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/material-orders")
public class MaterialOrderApiController {
	
    private static final Logger log = LoggerFactory.getLogger(MaterialOrderApiController.class);

    private final MaterialOrderService materialOrderService;
    private final MaterialSpecService materialSpecService;
    private final StyleService styleService;
    private final AgreementQueryService agreementQueryService;

    public MaterialOrderApiController(MaterialOrderService materialOrderService,
            MaterialSpecService materialSpecService,
            StyleService styleService,
            AgreementQueryService agreementQueryService) {
        this.materialOrderService = materialOrderService;
        this.materialSpecService = materialSpecService;
        this.styleService = styleService;
        this.agreementQueryService = agreementQueryService;
    }

    @GetMapping("/context")
    public MaterialOrderContextResponse loadContext(@RequestParam("styleCode") String styleCode) {
        String trimmed = StringUtils.hasText(styleCode) ? styleCode.trim() : null;
        if (!StringUtils.hasText(trimmed)) {
            return new MaterialOrderContextResponse(false, "품번을 입력하세요.",
                    java.util.List.of(), java.util.List.of(),
                    new MaterialSpecMatrixResponse(java.util.List.of(), java.util.List.of(), java.util.Map.of()));
        }
        boolean exists = styleService.existsByStyleCode(trimmed);
        if (!exists) {
            return new MaterialOrderContextResponse(false, "없는 품번입니다.",
                    java.util.List.of(), java.util.List.of(),
                    new MaterialSpecMatrixResponse(java.util.List.of(), java.util.List.of(), java.util.Map.of()));
        }
        MaterialSpecOptionsResponse options = materialSpecService.loadOptions(trimmed);
        return new MaterialOrderContextResponse(true, "품번이 확인되었습니다.",
                options.colors(), options.agreements(),
                new MaterialSpecMatrixResponse(java.util.List.of(), java.util.List.of(), java.util.Map.of()));
    }

    @GetMapping("/search")
    public MaterialOrderSearchResponse search(@RequestParam("styleCode") String styleCode,
            @RequestParam("agreementCode") String agreementCode,
            @RequestParam("colorCode") String colorCode) {
        MaterialOrderService.MaterialOrderSearchResult result =
                materialOrderService.searchOrders(styleCode, agreementCode, colorCode);
        return new MaterialOrderSearchResponse(
                result.getSuppliers(),
                result.getMaterialsToOrder(),
                new MaterialSpecMatrixResponse(java.util.List.of(), java.util.List.of(), java.util.Map.of()),
                agreementQueryService.findAgreementQuantities(agreementCode));
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> submitOrder(@RequestBody MaterialOrderRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        String orderedBy = principal != null ? principal.getEmpNo() : null;
        if (!StringUtils.hasText(orderedBy)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "로그인 사용자 정보가 없습니다."
            ));
        }
    	try {
    		Map<String, Object> result = materialOrderService.submitOrders(request, orderedBy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("material order submit failed", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", resolveErrorMessage(e)
            ));
        }
    }

    private String resolveErrorMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (!StringUtils.hasText(message)) {
            message = e.getMessage();
        }
        return StringUtils.hasText(message) ? message : "발주 처리 중 오류가 발생했습니다.";
    }
}
