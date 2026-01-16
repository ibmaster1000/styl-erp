package com.example.erp.controller;

import com.example.erp.controller.dto.MaterialSpecCodeView;
import com.example.erp.domain.Code;
import com.example.erp.service.CodeService;
import com.example.erp.service.MaterialSpecService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
public class CodesApiController {

    private final CodeService codeService;
    private final MaterialSpecService materialSpecService;

    public CodesApiController(CodeService codeService, MaterialSpecService materialSpecService) {
        this.codeService = codeService;
        this.materialSpecService = materialSpecService;
    }

    @GetMapping("/warehouses")
    public List<MaterialSpecCodeView> searchWarehouses(@RequestParam(name = "keyword", required = false) String keyword) {
        String search = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<Code> codes = codeService.searchActiveCodes("WAREHOUSE", search, search, search);
        return materialSpecService.filterCodes(codes, keyword);
    }
    
    @GetMapping("/customers")
    public List<MaterialSpecCodeView> searchCustomers(@RequestParam(name = "keyword", required = false) String keyword) {
        String search = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<Code> codes = codeService.searchActiveCodes("CUSTOMER", search, search, search);
        return materialSpecService.filterCodes(codes, keyword);
    }
}
