package com.cos.devblog.board.dto;

import com.cos.devblog.board.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String author;
    private String imageUrl;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int commentCount;

    public static PostResponse from(Post post) {
        PostResponse dto = new PostResponse();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setAuthor(post.getAuthor());
        dto.setImageUrl(post.getImageUrl());
        dto.setCategoryName(post.getCategory() != null ? post.getCategory().getName() : null);
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setCommentCount(post.getComments() != null ? post.getComments().size() : 0);
        return dto;
    }
}
