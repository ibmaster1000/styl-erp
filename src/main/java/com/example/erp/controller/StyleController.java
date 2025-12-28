package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.StyleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/style")
public class StyleController extends PageViewSupport {

    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @GetMapping
    public String list(Model model) {
    	populate(model, "품번(스타일) 관리", "style", "pages/style", styleService.findAll());
        return "layout/layout";
    }
}