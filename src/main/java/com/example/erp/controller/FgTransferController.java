package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.FgTransferService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fg/transfers")
public class FgTransferController extends PageViewSupport {

    private final FgTransferService fgTransferService;

    public FgTransferController(FgTransferService fgTransferService) {
        this.fgTransferService = fgTransferService;
    }

    @GetMapping
    public String list(Model model) {
    	populate(model, "완제품 이동", "transfer", "pages/fg-transfers", fgTransferService.findAll());
        return "layout/layout";
    }
}