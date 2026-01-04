package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.WorkOrder;
import com.example.erp.service.WorkOrderService;
import com.example.erp.service.StyleRuleService;
import com.example.erp.service.WorkOrderAttachmentService;
import com.example.erp.service.WorkOrderAttachmentService.AttachmentType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController extends PageViewSupport {

    private final WorkOrderService workOrderService;
    private final StyleRuleService styleRuleService;
    private final WorkOrderAttachmentService workOrderAttachmentService;

    public WorkOrderController(WorkOrderService workOrderService,
            StyleRuleService styleRuleService,
            WorkOrderAttachmentService workOrderAttachmentService) {
this.workOrderService = workOrderService;
this.styleRuleService = styleRuleService;
this.workOrderAttachmentService = workOrderAttachmentService;
}

@GetMapping
public String list(Model model) {
	 List<WorkOrder> workOrders = workOrderService.findAll();
     Set<String> styleNos = workOrders.stream()
             .map(WorkOrder::getStyleNo)
             .filter(v -> v != null && !v.isBlank())
             .collect(Collectors.toSet());
     Map<String, Map<String, List<String>>> styleRules = styleRuleService.findRulesByStyleNos(styleNos);
     Map<AttachmentType, List<com.example.erp.controller.dto.WorkOrderAttachmentView>> attachments = workOrderAttachmentService.fetchGroupedAttachments();

		populate(model, "작업 지시서", "workorder", "pages/work-orders", workOrders);
	     model.addAttribute("styleRules", styleRules);
     model.addAttribute("attachments", attachments);
     return "layout/layout";
 }
}
