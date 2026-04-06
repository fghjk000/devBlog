package com.cos.devblog.qna.dto;

import com.cos.devblog.qna.entity.Qna;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QnaResponse {
    private Long id;
    private String title;
    private String content;
    private String username;
    private LocalDateTime createdAt;
    private boolean hasAnswer;

    public static QnaResponse from(Qna qna) {
        QnaResponse dto = new QnaResponse();
        dto.setId(qna.getId());
        dto.setTitle(qna.getTitle());
        dto.setContent(qna.getContent());
        dto.setUsername(qna.getUsername());
        dto.setCreatedAt(qna.getCreatedAt());
        dto.setHasAnswer(!qna.getAnswers().isEmpty());
        return dto;
    }
}
