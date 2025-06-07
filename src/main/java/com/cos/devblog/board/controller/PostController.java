package com.cos.devblog.board.controller;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.entity.Comment;
import com.cos.devblog.board.entity.Post;
import com.cos.devblog.board.repository.CategoryRepository;
import com.cos.devblog.board.service.CategoryService;
import com.cos.devblog.board.service.ChatbotService;
import com.cos.devblog.board.service.CommentService;
import com.cos.devblog.board.service.PostService;
import com.cos.devblog.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class PostController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @GetMapping("/post")
    @PreAuthorize("hasRole('ADMIN')")
    public String createPost() {
        return "postForm";
    }

    @PostMapping("/post")
    @PreAuthorize("hasRole('ADMIN')")
    public String createPost(@RequestParam String title,
                             @RequestParam String content,
                             @RequestParam("image") MultipartFile image
                             ) throws IOException {

        postService.savePost(title, content, image);  // 예시로 postService.savePost() 메서드 사용
        return "redirect:/";  // 게시글 목록 페이지로 리다이렉트
    }

    @GetMapping("/posts")
    public String listPosts(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String contents,
                            Model model) {

        // 페이지 설정 (최신순 정렬)
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> posts;

        if ("제목".equals(type)) {
            posts = postService.searchByTitle(contents, pageable);
        } else {
            posts = postService.getAllPosts(pageable);
        }

        model.addAttribute("posts", posts);
        return "postList";
    }

    @GetMapping("/post/{id}")
    public String showPost(@PathVariable Long id, Model model) {
        Post post = postService.getDetail(id);
        List<Comment> comments = commentService.getComments(id);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments); // 댓글 목록 추가
        return "postDetail";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPost(Model model) {
        List<Post> posts = postService.getAll();
        model.addAttribute("posts", posts);
        return "postMyPage";
    }

    @DeleteMapping("/post/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "redirect:/admin";
    }

    @GetMapping("/post/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updatePost(@PathVariable Long id, Model model) {
        Post post = postService.getDetail(id);
        model.addAttribute("post", post);

        return "postUpdate";
    }

    @PutMapping("/post/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updatePost(@PathVariable Long id,
                             @RequestParam String title,
                             @RequestParam String content,
                             @RequestParam("image") MultipartFile image) throws IOException {
        postService.updatePost(id, title, content, image);
        return "redirect:/admin";
    }
}
