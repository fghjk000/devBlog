package com.cos.devblog.board.controller;

import com.cos.devblog.board.dto.PostResponse;
import com.cos.devblog.board.entity.Post;
import com.cos.devblog.board.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> posts = search != null && !search.isBlank()
                ? postService.searchByTitle(search, pageable)
                : postService.getAllPosts(pageable);

        return ResponseEntity.ok(posts.map(PostResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(PostResponse.from(postService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createPost(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        postService.savePost(title, content, image);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image) throws IOException {
        postService.updatePost(id, title, content, image);
        return ResponseEntity.ok(PostResponse.from(postService.getDetail(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
