package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;

@Controller
public class WorkOrderCreatePageController extends PageViewSupport {

    @GetMapping({"/work-order-create", "/work-orders/create"})
    public String view(Model model) {
        populate(model, "작업지시", "work-instructions", "pages/work-order-create", Collections.emptyList());
        return "layout/layout";
    }
}
