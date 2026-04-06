package com.cos.devblog.qna.service;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.repository.CategoryRepository;
import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.qna.repository.QnaRepository;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class QnaService {

    @Autowired
    private QnaRepository qnaRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    public Page<Qna> searchByTitle(String contents, Pageable pageable) {
        return qnaRepository.findByTitleContaining(contents, pageable);
    }

    public Page<Qna> getAllQna(Pageable pageable) {
        return qnaRepository.findAll(pageable);
    }

    public void qnaCreate(String title, String contents, User user) {
        // "Q&A" 카테고리를 찾거나 없으면 생성
        Category defaultCategory = categoryRepository.findByName("Q&A")
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName("Q&A");
                    return categoryRepository.save(newCategory);
                });

        // Qna 객체 생성 및 기본 카테고리 설정
        Qna qna = new Qna();
        qna.setTitle(title);
        qna.setContent(contents);
        qna.setUser(user);
        qna.setCategory(defaultCategory); // 자동으로 "Q&A" 카테고리 설정
        qna.setCreatedAt(LocalDateTime.now());

        qnaRepository.save(qna);
    }
    public Qna getQna(Long id) {
        return qnaRepository.findById(id).orElseThrow(()
                -> new RuntimeException("Post not found with id: " + id));
    }

    public Qna updateQna(Long id, String title, String content) {
        // 1. ID로 Qna 엔티티를 찾는다.
        Qna qna = qnaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Qna not found with id " + id));

        // 2. 수정할 필드를 업데이트한다.
        qna.setTitle(title);
        qna.setContent(content);
        qna.setUpdatedAt(LocalDateTime.now()); // 수정 시 updatedAt을 현재 시간으로 설정

        // 3. 업데이트된 Qna 엔티티를 저장한다.
        return qnaRepository.save(qna);
    }

    public List<Qna> getByUser(User user) {
        return qnaRepository.findByUser(user);
    }

    public void deleteQna(Long id) {
        qnaRepository.deleteById(id);
    }

    public Qna createQna(String title, String content, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category defaultCategory = categoryRepository.findByName("Q&A")
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName("Q&A");
                    return categoryRepository.save(newCategory);
                });
        Qna qna = new Qna();
        qna.setTitle(title);
        qna.setContent(content);
        qna.setUser(user);
        qna.setCategory(defaultCategory);
        qna.setCreatedAt(LocalDateTime.now());
        return qnaRepository.save(qna);
    }
}
