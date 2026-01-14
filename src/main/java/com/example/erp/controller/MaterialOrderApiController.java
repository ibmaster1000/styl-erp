package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialOrderContextResponse;
import com.example.erp.controller.dto.MaterialOrderRequest;
import com.example.erp.controller.dto.MaterialOrderSearchResponse;
import com.example.erp.controller.dto.MaterialSpecMatrixResponse;
import com.example.erp.controller.dto.MaterialSpecOptionsResponse;
import com.example.erp.service.MaterialOrderService;
import com.example.erp.service.MaterialSpecService;
import com.example.erp.service.StyleService;
import org.springframework.http.ResponseEntity;
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

    private final MaterialOrderService materialOrderService;
    private final MaterialSpecService materialSpecService;
    private final StyleService styleService;

    public MaterialOrderApiController(MaterialOrderService materialOrderService,
            MaterialSpecService materialSpecService,
            StyleService styleService) {
        this.materialOrderService = materialOrderService;
        this.materialSpecService = materialSpecService;
        this.styleService = styleService;
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
        MaterialSpecMatrixResponse matrix = materialSpecService.loadMatrix(trimmed);
        return new MaterialOrderContextResponse(true, "품번이 확인되었습니다.",
                options.colors(), options.agreements(), matrix);
    }

    @GetMapping("/search")
    public MaterialOrderSearchResponse search(@RequestParam("styleCode") String styleCode,
            @RequestParam("agreementCode") String agreementCode,
            @RequestParam("colorCode") String colorCode) {
        MaterialSpecMatrixResponse matrix = materialSpecService.loadMatrix(styleCode);
        MaterialOrderService.MaterialOrderSearchResult result =
                materialOrderService.searchOrders(styleCode, agreementCode, colorCode);
        return new MaterialOrderSearchResponse(
                result.getSuppliers(),
                result.getMaterialsToOrder(),
                matrix);
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> submitOrder(@RequestBody MaterialOrderRequest request) {
        Map<String, Object> result = materialOrderService.submitOrders(request, null);
        return ResponseEntity.ok(result);
    }
}
