package com.example.erp.controller;

import com.example.erp.controller.dto.StyleRegisterRequest;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.StyleRegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/styles/register")
public class StyleRegisterController extends PageViewSupport {

    private final StyleRegistrationService styleRegistrationService;

    public StyleRegisterController(StyleRegistrationService styleRegistrationService) {
        this.styleRegistrationService = styleRegistrationService;
    }

    @GetMapping
    public String view(Model model,
                       @ModelAttribute("form") StyleRegisterRequest form) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new StyleRegisterRequest());
        }
        model.addAttribute("items", styleRegistrationService.findActiveItems());
        populate(model, "품번 등록", "style", "pages/style-register", Collections.emptyList());
        return "layout/layout";
    }

    @PostMapping
    public String register(@ModelAttribute("form") StyleRegisterRequest form, Model model) {
        try {
            styleRegistrationService.createStyleWithRules(form);
            return "redirect:/style";
        } catch (RuntimeException e) {
            model.addAttribute("items", styleRegistrationService.findActiveItems());
            model.addAttribute("message", e.getMessage());
            populate(model, "품번 등록", "style", "pages/style-register", Collections.emptyList());
            return "layout/layout";
        }
    }
}
