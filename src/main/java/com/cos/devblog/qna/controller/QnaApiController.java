package com.cos.devblog.qna.controller;

import com.cos.devblog.qna.dto.QnaResponse;
import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.qna.service.AnswerService;
import com.cos.devblog.qna.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaApiController {

    private final QnaService qnaService;
    private final AnswerService answerService;

    @GetMapping
    public ResponseEntity<Page<QnaResponse>> getQnaList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        return ResponseEntity.ok(qnaService.getAllQna(pageable).map(QnaResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QnaResponse> getQna(@PathVariable Long id) {
        return ResponseEntity.ok(QnaResponse.from(qnaService.getQna(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QnaResponse> createQna(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String email) {
        Qna qna = qnaService.createQna(body.get("title"), body.get("content"), email);
        return ResponseEntity.status(201).body(QnaResponse.from(qna));
    }

    @PostMapping("/{id}/answer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String email) {
        answerService.createAnswer(id, body.get("content"), email);
        return ResponseEntity.status(201).build();
    }
}
