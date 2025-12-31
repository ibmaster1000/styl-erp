package com.example.erp.controller;

import com.example.erp.domain.Code;
import com.example.erp.domain.User;
import com.example.erp.service.CodeService;
import com.example.erp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/popup")
public class PopupController {

    private final CodeService codeService;
    private final UserService userService;

    public PopupController(CodeService codeService, UserService userService) {
        this.codeService = codeService;
        this.userService = userService;
    }

    @GetMapping("/codes")
    public String codeSearchPopup(@RequestParam(required = false) String codeType,
                                  @RequestParam(required = false) String code,
                                  @RequestParam(required = false) String codeName,
                                  @RequestParam(required = false) String remark,
                                  Model model) {
        List<Code> codes = codeService.searchActiveCodes(codeType, code, codeName, remark);
        List<String> codeTypes = codeService.findActiveCodeTypes();

        model.addAttribute("codes", codes);
        model.addAttribute("codeTypes", codeTypes);
        model.addAttribute("searchCodeType", codeType);
        model.addAttribute("searchCode", code);
        model.addAttribute("searchCodeName", codeName);
        model.addAttribute("searchRemark", remark);

        return "popup/code-search";
    }

    @GetMapping("/employees")
    public String employeeSearchPopup(@RequestParam(required = false) String empNo,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) String dept,
                                      Model model) {
        List<User> users = userService.searchActiveUsers(empNo, name, dept);
        List<String> departments = userService.findActiveDepartments();

        model.addAttribute("employees", users);
        model.addAttribute("departments", departments);
        model.addAttribute("searchEmpNo", empNo);
        model.addAttribute("searchName", name);
        model.addAttribute("searchDept", dept);

        return "popup/employee-search";
    }
}