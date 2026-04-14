package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.dto.PostRequest;
import com.hasa.linkedIn.Post.Generator.dto.PostResponse;
import com.hasa.linkedIn.Post.Generator.dto.GeneratePostRequest;
import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.PostStatus;
import com.hasa.linkedIn.Post.Generator.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest postRequest) {
        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setHashtags(postRequest.getHashtags());
        post.setImageUrl(postRequest.getImageUrl());
        post.setStatus(PostStatus.DRAFT);

        Post createdPost = postService.createDraft(post);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PostResponse.fromPost(createdPost));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        List<PostResponse> responses = posts.stream()
                .map(PostResponse::fromPost)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(post -> ResponseEntity.ok(PostResponse.fromPost(post)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id,
            @Valid @RequestBody PostRequest postRequest) {
        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setHashtags(postRequest.getHashtags());
        post.setImageUrl(postRequest.getImageUrl());

        // Note: You'll need to implement updatePost in PostService
        // For now, this is a placeholder
        Post updatedPost = postService.getPostById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return ResponseEntity.ok(PostResponse.fromPost(updatedPost));
    }

    @PutMapping("/{id}/schedule")
    public ResponseEntity<PostResponse> schedulePost(@PathVariable Long id,
            @RequestParam String time) {
        LocalDateTime scheduledTime = LocalDateTime.parse(time);
        Post post = postService.schedulePost(id, scheduledTime);
        return ResponseEntity.ok(PostResponse.fromPost(post));
    }

    @PostMapping("/generate")
    public ResponseEntity<PostResponse> generatePost(@Valid @RequestBody GeneratePostRequest generateRequest) {
        Post post = postService.generatePost(generateRequest.getPrompt(), generateRequest.getExistingContent());
        return ResponseEntity.ok(PostResponse.fromPost(post));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<PostResponse> publishPost(@PathVariable Long id) {
        Post post = postService.getPostById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setStatus(PostStatus.POSTED);
        return ResponseEntity.ok(PostResponse.fromPost(post));
    }
}