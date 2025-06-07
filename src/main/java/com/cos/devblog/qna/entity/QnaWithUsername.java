package com.cos.devblog.qna.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class QnaWithUsername {

    // getter, setter
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;

    // 생성자
    public QnaWithUsername(Qna qna, String username) {
        this.id = qna.getId();
        this.title = qna.getTitle();
        this.content = qna.getContent();
        this.createdAt = qna.getCreatedAt();
        this.updatedAt = qna.getUpdatedAt();
        this.username = username;
    }

}