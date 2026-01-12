package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialSpecCodeView;
import com.example.erp.controller.dto.MaterialSpecContextView;
import com.example.erp.controller.dto.MaterialSpecSaveRequest;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.Code;
import com.example.erp.service.CodeService;
import com.example.erp.service.MaterialSpecService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/production/material-specs")
public class MaterialSpecController extends PageViewSupport {

    private final MaterialSpecService materialSpecService;
    private final CodeService codeService;

    public MaterialSpecController(MaterialSpecService materialSpecService,
            CodeService codeService) {
        this.materialSpecService = materialSpecService;
        this.codeService = codeService;
    }

    @GetMapping
    public String list(Model model) {
        populate(model, "원부자재 사양서 등록", "material-specs", "pages/material-specs",
                List.of());
        return "layout/layout";
    }

    @GetMapping("/context")
    @ResponseBody
    public MaterialSpecContextView loadContext(@RequestParam("styleCode") String styleCode,
            @RequestParam("prdAgreeCode") String prdAgreeCode,
            @RequestParam("colorCode") String colorCode) {
        return materialSpecService.loadContext(styleCode, prdAgreeCode, colorCode);
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveSpecs(@RequestBody MaterialSpecSaveRequest request) {
        try {
            return ResponseEntity.ok(materialSpecService.saveSpecs(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "savedCount", 0, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("success", false, "savedCount", 0, "message", e.getMessage()));
        }
    }

    @GetMapping("/codes")
    @ResponseBody
    public List<MaterialSpecCodeView> loadCodes(@RequestParam("codeType") String codeType,
            @RequestParam(name = "keyword", required = false) String keyword) {
        List<Code> codes = codeService.searchActiveCodes(codeType, null, null, null);
        return materialSpecService.filterCodes(codes, keyword);
    }
}
