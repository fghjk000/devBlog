package com.cos.devblog.qna.controller;

import com.cos.devblog.config.auth.PrincipalDetails;
import com.cos.devblog.qna.entity.Answer;
import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.qna.repository.AnswerRepository;
import com.cos.devblog.qna.service.AnswerService;
import com.cos.devblog.qna.service.QnaService;
import com.cos.devblog.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller
public class AnswerController {

    @Autowired
    private AnswerService answerService;
    @Autowired
    private QnaService qnaService;
    @Autowired
    private AnswerRepository answerRepository;

    @PostMapping("/qna/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String answerQuestion(@PathVariable Long id,
                                 @RequestParam String content,
                                 Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 로그인되지 않은 상태라면 로그인 페이지로 리다이렉트
            return "redirect:/";  // 로그인 페이지로 리다이렉트
        }

        // 로그인된 경우, 인증된 사용자 정보 가져오기
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();  // PrincipalDetails에서 User를 가져옴

        answerService.answerCreate(id, user, content);
        model.addAttribute("question", id);
        return "redirect:/qna/" +id ;
    }

    @DeleteMapping("/answer/delete/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String deleteAnswer(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 로그인되지 않은 상태라면 로그인 페이지로 리다이렉트
            return "redirect:/";  // 로그인 페이지로 리다이렉트
        }
        Answer answer = answerRepository.findById(id).orElse(null);

        Qna qna = Objects.requireNonNull(answer).getQna();
        answerService.answerDelete(id);
        return "redirect:/qna/"+ qna.getId() ;
    }
}
