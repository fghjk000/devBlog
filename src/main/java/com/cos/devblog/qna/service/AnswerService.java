package com.cos.devblog.qna.service;

import com.cos.devblog.qna.entity.Answer;
import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.qna.repository.AnswerRepository;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnswerService {

    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private QnaService qnaService;
    @Autowired
    private UserRepository userRepository;

    public void createAnswer(Long qnaId, String content, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        answerCreate(qnaId, user, content);
    }

    public void answerCreate(Long id, User user, String content) {
        Qna qna = qnaService.getQna(id);
        Answer answer = new Answer();
        answer.setContent(content);
        answer.setUser(user);
        answer.setQna(qna);
        answer.setCreateAt(LocalDateTime.now());
        answerRepository.save(answer);

    }

    public void answerDelete(Long id) {
        answerRepository.deleteById(id);
    }
}
