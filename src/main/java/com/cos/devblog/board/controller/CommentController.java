package com.cos.devblog.board.controller;

import com.cos.devblog.config.auth.PrincipalDetails;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.board.service.CommentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/post/{id}/comment")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content) {
        // 로그인 여부 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 로그인되지 않은 상태라면 로그인 페이지로 리다이렉트
            return "redirect:/";  // 로그인 페이지로 리다이렉트
        }

        // 로그인된 경우, 인증된 사용자 정보 가져오기
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();  // PrincipalDetails에서 User를 가져옴

        // 댓글 추가
        commentService.addComment(id, user, content);

        // 댓글 작성 후 해당 게시글로 이동
        return "redirect:/post/" + id;
    }
}
