package com.cos.devblog.qna.entity;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // User 엔티티의 username을 반환하는 메서드 추가
    public String getUsername() {
        return this.user != null ? this.user.getUsername() : null;
    }

    @OneToMany(mappedBy = "qna", fetch = FetchType.LAZY)
    private List<Answer> answers = new ArrayList<>();

    // **Q&A 카테고리를 자동으로 설정하는 생성자 추가**
    public Qna(String title, String content, User user, Category defaultCategory) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.category = defaultCategory; // 자동으로 "Q&A" 카테고리 설정
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
