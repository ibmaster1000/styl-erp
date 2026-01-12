package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.WorkOrderService;
import com.example.erp.service.WorkOrderService.AttachmentType;
import com.example.erp.service.WorkOrderService.SaveRequest;
import com.example.erp.service.WorkOrderService.SaveResult;
import com.example.erp.service.WorkOrderService.WorkOrderDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController extends PageViewSupport {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public String list(@RequestParam(name = "styleCode", required = false) String styleCode, Model model) {
        WorkOrderDetail detail = workOrderService.loadByStyleCode(styleCode);

        populate(model, "작업 지시서", "workorder", "pages/work-orders", Collections.emptyList());
        model.addAttribute("styleCode", detail.styleCode());
        model.addAttribute("stylesId", detail.stylesId());
        model.addAttribute("orderId", detail.orderId());
        model.addAttribute("sizeCodes", detail.sizeCodes());
        model.addAttribute("sizeSpecs", detail.sizeSpecs());
        model.addAttribute("attachments", detail.attachments());
        model.addAttribute("styleNotFound", detail.notFound());
        return "layout/layout";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(@RequestBody SaveRequest request) {
        SaveResult result = workOrderService.saveSizeSpecs(request);
        return ResponseEntity.ok(Map.of(
                "success", result.success(),
                "message", result.message(),
                "orderId", result.orderId()
        ));
    }

    @DeleteMapping("/attachments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAttachment(@RequestParam("orderId") Long orderId,
            @RequestParam("type") String type) {
        AttachmentType attachmentType = AttachmentType.from(type);
        boolean success = workOrderService.deleteAttachment(orderId, attachmentType);
        return ResponseEntity.ok(Map.of("success", success));
    }
}
