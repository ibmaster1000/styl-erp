package com.example.erp.controller;

import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.Style;
import com.example.erp.service.MaterialSpecService;
import com.example.erp.service.StyleRuleService;
import com.example.erp.service.StyleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/production/material-specs")
public class MaterialSpecController extends PageViewSupport {

    private final MaterialSpecService materialSpecService;
    private final StyleService styleService;
    private final StyleRuleService styleRuleService;

    public MaterialSpecController(MaterialSpecService materialSpecService,
            StyleService styleService,
            StyleRuleService styleRuleService) {
        this.materialSpecService = materialSpecService;
        this.styleService = styleService;
        this.styleRuleService = styleRuleService;
    }

    @GetMapping
    public String list(Model model) {
        List<Style> styles = styleService.findAll();
        Set<String> styleNos = styles.stream()
                .map(Style::getStyleNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Map<String, List<String>>> styleRules = styleRuleService.findRulesByStyleNos(styleNos);

        populate(model, "원부자재 사양서 등록", "material-specs", "pages/material-specs",
                materialSpecService.findAllSorted());
        model.addAttribute("styles", styles);
        model.addAttribute("styleRules", styleRules);
        return "layout/layout";
    }
}