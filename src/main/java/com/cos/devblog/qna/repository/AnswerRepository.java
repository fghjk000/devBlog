package com.cos.devblog.qna.repository;

import com.cos.devblog.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}
