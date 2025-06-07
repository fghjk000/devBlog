package com.cos.devblog.board.service;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public void addCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setCreatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    public List<Category> getCategory() {
        return categoryRepository.findAll();
    }
}
