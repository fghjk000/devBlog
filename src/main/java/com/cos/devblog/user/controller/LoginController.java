package com.cos.devblog.user.controller;

import com.cos.devblog.user.dto.UserDto;
import com.cos.devblog.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;


    @GetMapping("/loginForm")
    public String loginForm() {
        return "loginForm";
    }

    @GetMapping("/joinForm")
    public String joinForm(Model model) {
        model.addAttribute("userDto", new UserDto()); // UserDto 객체를 모델에 추가
        return "joinForm";
    }

    @PostMapping("/join")
    public String join(@Valid UserDto userDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userDto", userDto); // 오류가 있으면 입력값을 다시 모델에 추가
            return "joinForm"; // 오류가 있으면 joinForm 페이지로 돌아감
        }
        userService.UserCreate(userDto); // 회원 가입 처리
        return "redirect:/loginForm"; // 가입 완료 후 로그인 폼으로 리다이렉트
    }

}
