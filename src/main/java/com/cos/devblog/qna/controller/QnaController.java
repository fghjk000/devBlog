package com.cos.devblog.qna.controller;

import com.cos.devblog.board.entity.Post;
import com.cos.devblog.config.auth.PrincipalDetails;
import com.cos.devblog.qna.entity.Answer;
import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.qna.entity.QnaWithUsername;
import com.cos.devblog.qna.service.AnswerService;
import com.cos.devblog.qna.service.QnaService;
import com.cos.devblog.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class QnaController {

    @Autowired
    private QnaService qnaService;

    @Autowired
    private AnswerService answerService;

    @GetMapping("/qna")
    public String QnaList(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String contents,
                          Model model) {

            // 페이지 설정 (최신순 정렬)
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt")));
            Page<Qna> qnas;

            if ("제목".equals(type)) {
                qnas = qnaService.searchByTitle(contents, pageable);
            } else {
                qnas = qnaService.getAllQna(pageable);
            }

            List<QnaWithUsername> qnaWithUsernames = qnas.getContent().stream()
                .map(qna -> new QnaWithUsername(qna, qna.getUsername()))  // QnaWithUsername DTO 객체 생성
                .toList();
            model.addAttribute("qnas", qnas);

            return "qnaList";
    }

    @GetMapping("/qna/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String addQna(){
        return "qnaCreate";
    }


    @PostMapping("/qna/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String addQna(@RequestParam String title,
                         @RequestParam String contents) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/qna";
        }

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();

        qnaService.qnaCreate(title, contents, user);

        return "redirect:/qna";
    }

    @GetMapping("/qna/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String Qna(@PathVariable Long id, @AuthenticationPrincipal PrincipalDetails principalDetails, Model model) {
        User user = principalDetails.getUser(); // 로그인한 사용자
        Qna qna = qnaService.getQna(id); // 해당 Qna 게시글 가져오기

        // 질문 작성자와 로그인한 사용자가 동일한지 비교
        boolean isBoardUser = qna.getUser().getId().equals(user.getId());

        // 답변 작성자 이름 추가 (중복되지 않도록 처리)
        List<String> answerUsernames = qna.getAnswers().stream()
                .map(answer -> answer.getUser().getUsername())
                .distinct()  // 중복 제거
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("answers", qna.getAnswers());
        model.addAttribute("qna", qna);
        model.addAttribute("boarduser", isBoardUser); // 로그인한 사용자와 게시글 작성자가 동일하면 true
        model.addAttribute("answerUsernames", answerUsernames); // 답변 작성자 이름들

        return "qnaDetail";
    }

    @GetMapping("/qna/update/{id}")
    public String updateQnaGet(@PathVariable Long id, Model model) {
        Qna qna = qnaService.getQna(id);
        // 로그인한 사용자가 qna의 작성자이거나 관리자일 경우만 접근 허용
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!qna.getUsername().equals(username)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        model.addAttribute("qna", qna);
        return "qnaUpdate";
    }

    @PutMapping("/qna/update/{id}")
    public String updateQna(@PathVariable Long id,
                            @RequestParam String title,
                            @RequestParam String content,
                            Model model) {
        Qna qna = qnaService.updateQna(id, title, content);
        // 로그인한 사용자가 qna의 작성자이거나 관리자일 경우만 접근 허용
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!qna.getUsername().equals(username)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        model.addAttribute("qna", qna);
        return "redirect:/qna/"+id;
    }

    @GetMapping("/qna/manage")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String manageQna(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 로그인되지 않은 상태라면 로그인 페이지로 리다이렉트
            return "redirect:/";  // 로그인 페이지로 리다이렉트
        }

        // 로그인된 경우, 인증된 사용자 정보 가져오기
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();  // PrincipalDetails에서 User를 가져옴

        List<Qna> Qnas = qnaService.getByUser(user);
        model.addAttribute("Qnas", Qnas);

        return "qnaManage";
    }

    @DeleteMapping("/qna/delete/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String deleteQna(@PathVariable Long id) {
        qnaService.deleteQna(id);
        return "redirect:/qna/manage";
    }
}
