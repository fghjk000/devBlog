package com.cos.devblog.board.controller;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/category")
    @PreAuthorize("hasRole('ADMIN')")
    public String category(Model model) {
        List<Category> categories = categoryService.getCategory();
        model.addAttribute("categories", categories);
        return "category";
    }

    @PostMapping("/category")
    @PreAuthorize("hasRole('ADMIN')")
    public String addCategory(@RequestParam("name") String name){
        categoryService.addCategory(name);
        return "redirect:/";
    }
}
