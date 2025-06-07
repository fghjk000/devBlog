package com.cos.devblog.board.service;

import com.cos.devblog.board.entity.Comment;
import com.cos.devblog.board.entity.Post;
import com.cos.devblog.user.entity.User;
import com.cos.devblog.board.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostService postService;

    public void addComment(Long id, User user, String content) {
        Post post = postService.getPost(id);

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostId(postId);
    }
}
