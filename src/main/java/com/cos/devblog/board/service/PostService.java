package com.cos.devblog.board.service;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.entity.Post;
import com.cos.devblog.board.repository.CategoryRepository;
import com.cos.devblog.board.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public List<Post> getRecentPosts() {
        return postRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public void savePost(String title, String content, MultipartFile image) throws IOException {
        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path path = Paths.get(uploadDir, fileName);
            Files.createDirectories(path.getParent());
            image.transferTo(path.toFile());
            imageUrl = fileName;
        }

        Category defaultCategory = categoryRepository.findByName("게시글")
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName("게시글");
                    return categoryRepository.save(newCategory);
                });

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(defaultCategory);
        post.setImageUrl(imageUrl);
        post.setCreatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    public Page<Post> searchByTitle(String contents, Pageable pageable) {
        return postRepository.findByTitleContaining(contents, pageable);
    }

    public Page<Post> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    public Post getDetail(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
    }

    public Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
    }

    public List<Post> getAll() {
        return postRepository.findAll();
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public void updatePost(Long postId, String title, String content, MultipartFile image) throws IOException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(LocalDateTime.now());

        String rawFileName = post.getRawImageFileName();

        if (image != null && !image.isEmpty()) {
            if (rawFileName != null && !rawFileName.isEmpty()) {
                Path oldImagePath = Paths.get(uploadDir, rawFileName);
                Files.deleteIfExists(oldImagePath);
            }
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path path = Paths.get(uploadDir, fileName);
            Files.createDirectories(path.getParent());
            image.transferTo(path.toFile());
            rawFileName = fileName;
        }

        post.setImageUrl(rawFileName);
        postRepository.save(post);
    }
}
