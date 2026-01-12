package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialSpecContextView;
import com.example.erp.controller.dto.MaterialSpecMatrixResponse;
import com.example.erp.controller.dto.MaterialSpecOptionsResponse;
import com.example.erp.controller.dto.MaterialSpecVerifyResponse;
import com.example.erp.service.MaterialSpecService;
import com.example.erp.service.StyleService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/materials/spec")
public class MaterialSpecApiController {

    private final MaterialSpecService materialSpecService;
    private final StyleService styleService;

    public MaterialSpecApiController(MaterialSpecService materialSpecService, StyleService styleService) {
        this.materialSpecService = materialSpecService;
        this.styleService = styleService;
    }

    @GetMapping("/verify")
    public MaterialSpecVerifyResponse verifyStyle(@RequestParam("styleCode") String styleCode) {
        if (!StringUtils.hasText(styleCode)) {
            return new MaterialSpecVerifyResponse(false, "품번을 입력하세요.");
        }
        boolean exists = styleService.existsByStyleCode(styleCode.trim());
        return new MaterialSpecVerifyResponse(exists, exists ? "품번이 확인되었습니다." : "없는 품번입니다.");
    }

    @GetMapping("/options")
    public MaterialSpecOptionsResponse loadOptions(@RequestParam("styleCode") String styleCode) {
        return materialSpecService.loadOptions(styleCode);
    }

    @GetMapping("/matrix")
    public MaterialSpecMatrixResponse loadMatrix(@RequestParam("styleCode") String styleCode) {
        return materialSpecService.loadMatrix(styleCode);
    }

    @GetMapping("/search")
    public MaterialSpecContextView search(@RequestParam("styleCode") String styleCode,
            @RequestParam("color") String color,
            @RequestParam("agreementCode") String agreementCode) {
        return materialSpecService.loadContext(styleCode, agreementCode, color);
    }
}
