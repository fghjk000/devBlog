package com.cos.devblog.board.repository;

import com.cos.devblog.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findTop5ByOrderByCreatedAtDesc();

    Page<Post> findByTitleContaining(String title, Pageable pageable);

}
