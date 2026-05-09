package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.dto.PostRequest;
import com.hasa.linkedIn.Post.Generator.dto.PostResponse;
import com.hasa.linkedIn.Post.Generator.dto.GeneratePostRequest;
import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.PostStatus;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.service.PostService;
import com.hasa.linkedIn.Post.Generator.service.LinkedInShareService;
import com.hasa.linkedIn.Post.Generator.service.UserService;
import com.hasa.linkedIn.Post.Generator.config.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

        private static final Logger logger = LoggerFactory.getLogger(PostController.class);
        private final PostService postService;
        private final LinkedInShareService linkedInShareService;
        private final UserService userService;
        private final JwtUtil jwtUtil;

        public PostController(PostService postService,
                        LinkedInShareService linkedInShareService,
                        UserService userService,
                        JwtUtil jwtUtil) {
                this.postService = postService;
                this.linkedInShareService = linkedInShareService;
                this.userService = userService;
                this.jwtUtil = jwtUtil;
        }

        @PostMapping
        public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest postRequest) {
                User user = getCurrentUser();
                Post post = new Post();
                post.setTitle(postRequest.getTitle());
                post.setContent(postRequest.getContent());
                post.setHashtags(postRequest.getHashtags());
                post.setImageUrl(postRequest.getImageUrl());
                post.setStatus(PostStatus.DRAFT);

                Post createdPost = postService.createDraftForUser(post, user);
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(PostResponse.fromPost(createdPost));
        }

        @GetMapping
        public ResponseEntity<List<PostResponse>> getAllPosts(@RequestParam(required = false) String status) {
                User user = getCurrentUser();
                List<Post> posts;

                if (status == null || status.isBlank()) {
                        posts = postService.getPostsForUser(user);
                } else if ("NON_PUBLISHED".equalsIgnoreCase(status)) {
                        posts = postService.getNonPublishedPostsForUser(user);
                } else {
                        try {
                                PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
                                posts = postService.getPostsForUserByStatus(user, postStatus);
                        } catch (IllegalArgumentException ex) {
                                return ResponseEntity.badRequest().build();
                        }
                }

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
                User user = getCurrentUser();
                postService.deletePost(id, user);
                return ResponseEntity.noContent().build();
        }

        @PutMapping("/{id}")
        public ResponseEntity<PostResponse> updatePost(@PathVariable Long id,
                        @Valid @RequestBody PostRequest postRequest) {
                User user = getCurrentUser();
                Post post = new Post();
                post.setTitle(postRequest.getTitle());
                post.setContent(postRequest.getContent());
                post.setHashtags(postRequest.getHashtags());
                post.setImageUrl(postRequest.getImageUrl());

                Post updatedPost = postService.updatePost(id, post, user);

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

        /**
         * Shares a post to LinkedIn.
         * 
         * @param id Post ID to share
         * @return Response with share status
         */
        @PostMapping("/{id}/share-linkedin")
        public ResponseEntity<?> shareToLinkedIn(@PathVariable Long id) {
                try {
                        logger.info("Received request to share post {} to LinkedIn", id);

                        // Get current authenticated user
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        String email = authentication.getName();
                        logger.debug("Current authenticated user: {}", email);

                        // Get user from database
                        User user = userService.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // Check if user has LinkedIn connected
                        if (!user.getLinkedinConnected() || user.getLinkedinAccessToken() == null) {
                                logger.warn("User {} does not have LinkedIn connected", user.getId());
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of(
                                                                "success", false,
                                                                "message",
                                                                "LinkedIn account is not connected. Please connect your LinkedIn account first."));
                        }

                        // Get the post
                        Post post = postService.getPostById(id)
                                        .orElseThrow(() -> new RuntimeException("Post not found"));

                        // Share to LinkedIn
                        LinkedInShareService.LinkedInShareResponse shareResponse = linkedInShareService
                                        .sharePostToLinkedIn(user, post.getContent());

                        if (shareResponse.isSuccess()) {
                                logger.info("Post {} successfully shared to LinkedIn with ID: {}", id,
                                                shareResponse.getShareId());

                                // Update post status to POSTED
                                post.setStatus(PostStatus.POSTED);
                                post.setPublishedAt(LocalDateTime.now());
                                postService.updatePost(post);

                                return ResponseEntity.ok(Map.of(
                                                "success", true,
                                                "message", "Post shared successfully to LinkedIn",
                                                "shareId", shareResponse.getShareId()));
                        } else {
                                logger.warn("Failed to share post {} to LinkedIn: {}", id, shareResponse.getMessage());
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of(
                                                                "success", false,
                                                                "message", shareResponse.getMessage()));
                        }

                } catch (RuntimeException e) {
                        logger.error("Error sharing post to LinkedIn", e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of(
                                                        "success", false,
                                                        "message", e.getMessage()));
                } catch (Exception e) {
                        logger.error("Unexpected error while sharing to LinkedIn", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of(
                                                        "success", false,
                                                        "message", "An unexpected error occurred: " + e.getMessage()));
                }
        }

        private User getCurrentUser() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || authentication.getName() == null) {
                        throw new RuntimeException("Unauthorized");
                }

                String email = authentication.getName();
                return userService.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));
        }

        /**
         * Shares a post to LinkedIn with custom commentary.
         * 
         * @param id      Post ID to share
         * @param request Request body containing commentary
         * @return Response with share status
         */
        @PostMapping("/{id}/share-linkedin-with-commentary")
        public ResponseEntity<?> shareToLinkedInWithCommentary(
                        @PathVariable Long id,
                        @RequestBody Map<String, String> request) {
                try {
                        logger.info("Received request to share post {} to LinkedIn with commentary", id);

                        String commentary = request.get("commentary");
                        if (commentary == null || commentary.trim().isEmpty()) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of(
                                                                "success", false,
                                                                "message", "Commentary is required"));
                        }

                        // Get current authenticated user
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        String email = authentication.getName();

                        // Get user from database
                        User user = userService.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // Check if user has LinkedIn connected
                        if (!user.getLinkedinConnected() || user.getLinkedinAccessToken() == null) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of(
                                                                "success", false,
                                                                "message", "LinkedIn account is not connected"));
                        }

                        // Get the post
                        Post post = postService.getPostById(id)
                                        .orElseThrow(() -> new RuntimeException("Post not found"));

                        // Share to LinkedIn with commentary
                        LinkedInShareService.LinkedInShareResponse shareResponse = linkedInShareService
                                        .sharePostToLinkedInWithCommentary(
                                                        user,
                                                        post.getContent(),
                                                        commentary);

                        if (shareResponse.isSuccess()) {
                                logger.info("Post {} successfully shared to LinkedIn with commentary", id);

                                // Update post status to POSTED
                                post.setStatus(PostStatus.POSTED);
                                post.setPublishedAt(LocalDateTime.now());
                                postService.updatePost(post);

                                return ResponseEntity.ok(Map.of(
                                                "success", true,
                                                "message", "Post shared successfully to LinkedIn with commentary",
                                                "shareId", shareResponse.getShareId()));
                        } else {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of(
                                                                "success", false,
                                                                "message", shareResponse.getMessage()));
                        }

                } catch (Exception e) {
                        logger.error("Error sharing post with commentary to LinkedIn", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of(
                                                        "success", false,
                                                        "message", "An error occurred: " + e.getMessage()));
                }
        }

        /**
         * Creates a post directly on LinkedIn using the authenticated user's LinkedIn
         * token.
         * 
         * This endpoint:
         * 1. Validates JWT token from Authorization header
         * 2. Extracts user ID from JWT
         * 3. Fetches user and their LinkedIn access token from DB
         * 4. Calls LinkedIn UGC Posts API to create the post
         * 
         * @param authHeader Authorization header containing Bearer JWT
         * @param request    Request body containing post text
         * @return Response with success status or error message
         */
        @PostMapping("/linkedin/post")
        public ResponseEntity<?> createLinkedInPost(
                        @RequestHeader("Authorization") String authHeader,
                        @RequestBody Map<String, String> request) {

                try {
                        // Validate header exists
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                                logger.warn("Missing or invalid Authorization header");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("error", "Missing or invalid Authorization header"));
                        }

                        // Extract token
                        String token = authHeader.substring(7);

                        // Validate JWT token
                        if (!jwtUtil.validateToken(token)) {
                                logger.warn("Invalid JWT token");
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("error", "Invalid JWT token"));
                        }

                        // Get user ID from token
                        Long userId = jwtUtil.extractUserId(token);
                        logger.info("Processing LinkedIn post request for user: {}", userId);

                        // Fetch user from database
                        User user = userService.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // Check if LinkedIn is connected
                        if (user.getLinkedinAccessToken() == null || user.getLinkedinAccessToken().isEmpty()) {
                                logger.warn("User {} does not have LinkedIn connected", userId);
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error",
                                                                "LinkedIn account not connected. Please connect your LinkedIn account first."));
                        }

                        // Check if LinkedIn token is expired
                        if (user.getLinkedinTokenExpiry() != null &&
                                        LocalDateTime.now().isAfter(user.getLinkedinTokenExpiry())) {
                                logger.warn("LinkedIn access token expired for user {}", userId);
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error",
                                                                "LinkedIn token expired. Please reconnect your account."));
                        }

                        // Get post text
                        String text = request.get("text");
                        if (text == null || text.trim().isEmpty()) {
                                logger.warn("Empty post text");
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error", "Post text is required"));
                        }

                        // Share to LinkedIn
                        LinkedInShareService.LinkedInShareResponse shareResponse = linkedInShareService
                                        .sharePostToLinkedIn(user, text);

                        if (shareResponse.isSuccess()) {
                                logger.info("Post successfully published to LinkedIn for user: {}", userId);
                                return ResponseEntity.ok(Map.of(
                                                "success", true,
                                                "message", "Post published successfully to LinkedIn",
                                                "shareId", shareResponse.getShareId()));
                        } else {
                                logger.error("Failed to publish post to LinkedIn: {}", shareResponse.getMessage());
                                return ResponseEntity.badRequest()
                                                .body(Map.of(
                                                                "success", false,
                                                                "error", shareResponse.getMessage()));
                        }

                } catch (RuntimeException e) {
                        logger.error("Runtime error while creating LinkedIn post", e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", e.getMessage()));
                } catch (Exception e) {
                        logger.error("Unexpected error while creating LinkedIn post", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
                }
        }
}