package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.FgReceiptService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fg/receipts")
public class FgReceiptController extends PageViewSupport {

    private final FgReceiptService fgReceiptService;

    public FgReceiptController(FgReceiptService fgReceiptService) {
        this.fgReceiptService = fgReceiptService;
    }

    @GetMapping
    public String list(Model model) {
        populate(model, "완제품 입고", "receipt", "pages/fg-receipts :: content", fgReceiptService.findAll());
        return "layout/layout";
    }
}