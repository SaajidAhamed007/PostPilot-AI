package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.dto.GeneratePostRequest;
import com.hasa.linkedIn.Post.Generator.dto.PostRequest;
import com.hasa.linkedIn.Post.Generator.dto.PostResponse;
import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.repository.UserRepository;
import com.hasa.linkedIn.Post.Generator.service.PostService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    public PostController(
            PostService postService,
            UserRepository userRepository) {

        this.postService = postService;
        this.userRepository = userRepository;
    }

    // =========================
    // 1. SAVE AS DRAFT
    // =========================

    @PostMapping
    public ResponseEntity<PostResponse> createDraft(
            @Valid @RequestBody PostRequest request) {

        User user = getCurrentUser();

        Post post = buildPost(request);

        Post createdPost =
                postService.createDraftForUser(post, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PostResponse.fromPost(createdPost));
    }

    // =========================
    // 2. PUBLISH NOW
    // =========================

    @PostMapping("/publish-now")
    public ResponseEntity<PostResponse> publishNow(
            @Valid @RequestBody PostRequest request) {

        User user = getCurrentUser();

        Post post = buildPost(request);

        Post publishedPost =
                postService.publishNow(post, user);

        return ResponseEntity.ok(
                PostResponse.fromPost(publishedPost));
    }

    // =========================
    // 3. AI GENERATE POST
    // =========================

    @PostMapping("/generate")
    public ResponseEntity<PostResponse> generatePost(
            @Valid @RequestBody GeneratePostRequest request) {

        Post post =
                postService.generatePost(
                        request.getPrompt(),
                        request.getExistingContent());

        return ResponseEntity.ok(
                PostResponse.fromPost(post));
    }

    // =========================
    // 4. GET ALL MY POSTS
    // =========================

    @GetMapping
    public ResponseEntity<List<PostResponse>> getMyPosts() {

        User user = getCurrentUser();

        List<PostResponse> posts =
                postService.getPostsForUser(user)
                        .stream()
                        .map(PostResponse::fromPost)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(posts);
    }

    // =========================
    // 5. GET SINGLE POST
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable Long id) {

        User user = getCurrentUser();

        Post post =
                postService.getPostByIdForUser(id, user);

        return ResponseEntity.ok(
                PostResponse.fromPost(post));
    }

    // =========================
    // 6. UPDATE POST
    // =========================

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request) {

        User user = getCurrentUser();

        Post updatedPostData = buildPost(request);

        Post updatedPost =
                postService.updatePost(
                        id,
                        updatedPostData,
                        user);

        return ResponseEntity.ok(
                PostResponse.fromPost(updatedPost));
    }

    // =========================
    // 7. DELETE POST
    // =========================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id) {

        User user = getCurrentUser();

        postService.deletePost(id, user);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // HELPER METHOD
    // =========================

    private Post buildPost(PostRequest request) {

        Post post = new Post();

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setHashtags(request.getHashtags());
        post.setImageUrl(request.getImageUrl());

        return post;
    }

    // =========================
    // GET AUTHENTICATED USER
    // =========================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getName() == null) {

            throw new RuntimeException("Unauthorized");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }
}