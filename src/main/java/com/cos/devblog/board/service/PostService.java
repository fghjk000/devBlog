package com.cos.devblog.board.service;

import com.cos.devblog.board.entity.Category;
import com.cos.devblog.board.entity.Post;
import com.cos.devblog.board.repository.CategoryRepository;
import com.cos.devblog.board.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
    private Environment env;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Post> getRecentPosts() {
        // 최근 5개의 게시글을 가져오기
        List<Post> posts = postRepository.findTop5ByOrderByCreatedAtDesc();

        // 각 게시글의 imageUrl을 출력
        posts.forEach(post -> {
            System.out.println("Original imageUrl: " + post.getImageUrl());
        });

        return posts;
    }

    public void savePost(String title, String content, MultipartFile image) throws IOException {
        // 설정에서 경로 가져오기
//        String uploadDir = "/Users/kimhanseop/Desktop/devBlog/src/main/resources/static/uploads"; // 로컬 환경
        String uploadDir = "/app/uploads";
        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            // 파일 이름 생성
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();

            // 저장할 경로 설정
            Path path = Paths.get(uploadDir, fileName);
            File destination = path.toFile();

            // 파일 저장
            image.transferTo(destination);

            // 이미지 URL 경로 설정 (중복된 '/uploads/'를 방지)
            imageUrl = fileName; // 여기에서 중복 제거
        }

        Category defaultCategory = categoryRepository.findByName("게시글")
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName("게시글");
                    return categoryRepository.save(newCategory);
                });

        // 게시글 객체 생성 후 저장
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(defaultCategory);
        post.setImageUrl(imageUrl);  // 이미지 URL 설정
        post.setCreatedAt(LocalDateTime.now());
        postRepository.save(post);  // 게시글 저장
    }


    public Page<Post> searchByTitle(String contents, Pageable pageable) {
        return postRepository.findByTitleContaining(contents, pageable);
    }

    public Page<Post> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    public Post getDetail(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id)); // 예외를 던짐
    }

    public Post getPost(Long id) {
        return postRepository.findById(id).orElseThrow(()
                -> new RuntimeException("Post not found with id: " + id));
    }

    public List<Post> getAll() {
        return postRepository.findAll();
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public void updatePost(Long postId, String title, String content, MultipartFile image) throws IOException {
        // 1. 수정할 게시글 찾기
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));

        // 2. 제목과 내용 수정
        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(LocalDateTime.now());  // 수정 시간을 업데이트

        String imageUrl = post.getImageUrl();  // 기존 이미지 URL

        // 3. 새 이미지가 있다면 처리
        if (image != null && !image.isEmpty()) {
            // 기존 이미지 삭제 (기존 이미지 파일을 물리적으로 삭제하는 로직 추가)
            if (imageUrl != null && !imageUrl.isEmpty()) {
//                Path oldImagePath = Paths.get("/Users/kimhanseop/Desktop/devBlog/src/main/resources/static/uploads", imageUrl); // 로컬 환경
                Path oldImagePath = Paths.get("/app/uploads", imageUrl);
                Files.deleteIfExists(oldImagePath); // 기존 이미지 파일 삭제
            }

            // 새 이미지 저장
//            String uploadDir = "/Users/kimhanseop/Desktop/devBlog/src/main/resources/static/uploads"; // 로컬 환경
            String uploadDir = "/app/uploads";
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path path = Paths.get(uploadDir, fileName);
            File destination = path.toFile();
            image.transferTo(destination);

            imageUrl = fileName;  // 새 이미지 URL 설정
        }

        post.setImageUrl(imageUrl);  // 새로운 이미지 URL 설정

        // 4. 게시글 저장
        postRepository.save(post);
    }
}