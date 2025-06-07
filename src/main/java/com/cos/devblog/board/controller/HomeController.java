package com.cos.devblog.board.controller;

import com.cos.devblog.board.entity.Post;
import com.cos.devblog.board.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private PostService postService;

    @GetMapping("/")
    public String home(Model model) {
        List<Post> posts = postService.getRecentPosts(); // 최신 게시글 가져오기
        model.addAttribute("posts", posts);
        return "index"; // index.html로 이동
    }
}