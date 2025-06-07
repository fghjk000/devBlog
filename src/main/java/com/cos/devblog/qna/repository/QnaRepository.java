package com.cos.devblog.qna.repository;

import com.cos.devblog.qna.entity.Qna;
import com.cos.devblog.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    Page<Qna> findByTitleContaining(String contents, Pageable pageable);

    List<Qna> findByUser(User user);
}
