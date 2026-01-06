package com.example.erp.controller;

import com.example.erp.controller.dto.WorkOrderAgreementDetail;
import com.example.erp.controller.dto.WorkOrderAgreementSummary;
import com.example.erp.controller.dto.WorkOrderDetailSavePayload;
import com.example.erp.controller.dto.WorkOrderDispatchRequest;
import com.example.erp.service.WorkOrderInstructionService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderInstructionApiController {

    private final WorkOrderInstructionService workOrderInstructionService;

    public WorkOrderInstructionApiController(WorkOrderInstructionService workOrderInstructionService) {
        this.workOrderInstructionService = workOrderInstructionService;
    }

    @GetMapping("/{styleNo}/filters")
    public Map<String, List<String>> loadFilters(@PathVariable String styleNo) {
        return workOrderInstructionService.findFilters(styleNo);
    }

    @GetMapping("/{styleNo}/agreements")
    public List<WorkOrderAgreementSummary> loadAgreements(@PathVariable String styleNo) {
        return workOrderInstructionService.findAgreementSummaries(styleNo);
    }

    @GetMapping("/{styleNo}/agreements/detail")
    public List<WorkOrderAgreementDetail> loadAgreementDetails(@PathVariable String styleNo,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String agreementCode,
            @RequestParam(required = false) String empNo) {
        return workOrderInstructionService.findAgreementDetails(styleNo, color, agreementCode);
    }

    @PostMapping("/dispatch")
    public ResponseEntity<Map<String, Object>> dispatch(@RequestBody WorkOrderDispatchRequest request) {
        boolean success = workOrderInstructionService.dispatch(request);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "요청 정보를 확인해주세요."));
    }

    @PostMapping("/detail/save")
    public ResponseEntity<Map<String, Object>> saveDetails(@RequestBody(required = false) WorkOrderDetailSavePayload payload) {
        int saved = workOrderInstructionService.saveDetails(
                payload != null ? payload.getItems() : Collections.emptyList());
        boolean success = saved > 0 || (payload != null && CollectionUtils.isEmpty(payload.getItems()));
        return ResponseEntity.ok(Map.of("success", success, "savedCount", saved));
    }
}
