package com.example.erp.controller;

import com.example.erp.controller.dto.SignupForm;
import com.example.erp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String register(
            @Valid @ModelAttribute("signupForm") SignupForm signupForm,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            userService.registerUser(signupForm);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
            return "signup";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "회원가입 처리 중 오류가 발생했습니다.");
            return "signup";
        }

        return "redirect:/login?registered";
    }
}