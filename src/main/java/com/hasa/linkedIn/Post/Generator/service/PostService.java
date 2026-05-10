package com.hasa.linkedIn.Post.Generator.service;

import com.hasa.linkedIn.Post.Generator.integration.GeminiClient;
import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.PostStatus;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.repository.PostRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final GeminiClient geminiClient;
    private final LinkedInShareService linkedInShareService;

    public PostService(
            PostRepository postRepository,
            GeminiClient geminiClient,
            LinkedInShareService linkedInShareService) {

        this.postRepository = postRepository;
        this.geminiClient = geminiClient;
        this.linkedInShareService = linkedInShareService;
    }

    // =========================
    // CREATE DRAFT
    // =========================

    public Post createDraftForUser(Post post, User user) {

        post.setStatus(PostStatus.DRAFT);

        post.setCreatedAt(LocalDateTime.now());

        post.setUser(user);

        return postRepository.save(post);
    }

    // =========================
    // PUBLISH NOW
    // =========================

    public Post publishNow(Post post, User user) {

        post.setStatus(PostStatus.DRAFT);

        post.setCreatedAt(LocalDateTime.now());

        post.setUser(user);

        Post savedPost = postRepository.save(post);

        String fullContent = savedPost.getContent();

        if (savedPost.getHashtags() != null &&
                !savedPost.getHashtags().isBlank()) {

            fullContent += "\n\n" +
                    savedPost.getHashtags();
        }

        boolean success =
                linkedInShareService.publishToLinkedIn(
                        user,
                        fullContent);

        if (success) {

            savedPost.setStatus(PostStatus.POSTED);

            savedPost.setPublishedAt(
                    LocalDateTime.now());

            return postRepository.save(savedPost);
        }

        // If publish fails,
        // keep it as DRAFT

        return savedPost;
    }

    // =========================
    // AI GENERATE POST
    // =========================

    public Post generatePost(
            String prompt,
            String existingContent) {

        String aiResponse =
                geminiClient.generatePost(
                        prompt,
                        existingContent);

        String title = "";
        String content = "";
        String hashtags = "";

        String[] parts =
                aiResponse.split("Content:");

        if (parts.length > 1) {

            title = parts[0]
                    .replace("Title:", "")
                    .trim();

            String[] contentParts =
                    parts[1].split("Hashtags:");

            content = contentParts[0].trim();

            if (contentParts.length > 1) {

                hashtags =
                        contentParts[1].trim();
            }
        }

        Post post = new Post();

        post.setTitle(title);

        post.setContent(content);

        post.setHashtags(hashtags);

        return post;
    }

    // =========================
    // GET ALL POSTS FOR USER
    // =========================

    public List<Post> getPostsForUser(User user) {

        return postRepository.findByUser(user);
    }

    // =========================
    // GET POST BY ID
    // =========================

    public Post getPostByIdForUser(
            Long id,
            User user) {

        Post post = postRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Post not found"));

        if (post.getUser() == null ||
                !post.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new RuntimeException(
                    "Unauthorized access");
        }

        return post;
    }

    // =========================
    // UPDATE POST
    // =========================

    public Post updatePost(
            Long id,
            Post updatedPost,
            User user) {

        Post existingPost =
                getPostByIdForUser(id, user);

        if (existingPost.getStatus() ==
                PostStatus.POSTED) {

            throw new RuntimeException(
                    "Published posts cannot be edited");
        }

        existingPost.setTitle(
                updatedPost.getTitle());

        existingPost.setContent(
                updatedPost.getContent());

        existingPost.setHashtags(
                updatedPost.getHashtags());

        existingPost.setImageUrl(
                updatedPost.getImageUrl());

        return postRepository.save(existingPost);
    }

    // =========================
    // DELETE POST
    // =========================

    public void deletePost(
            Long id,
            User user) {

        Post existingPost =
                getPostByIdForUser(id, user);

        if (existingPost.getStatus() ==
                PostStatus.POSTED) {

            throw new RuntimeException(
                    "Published posts cannot be deleted");
        }

        postRepository.delete(existingPost);
    }

    // =========================
    // SCHEDULE POST
    // =========================

    public Post schedulePost(
            Long id,
            LocalDateTime scheduledTime,
            User user) {

        Post post =
                getPostByIdForUser(id, user);

        if (post.getStatus() ==
                PostStatus.POSTED) {

            throw new RuntimeException(
                    "Published posts cannot be scheduled");
        }

        post.setScheduledTime(scheduledTime);

        post.setStatus(PostStatus.SCHEDULED);

        return postRepository.save(post);
    }

    // =========================
    // GET SCHEDULED POSTS
    // =========================

    public List<Post> getScheduledPosts() {

        return postRepository.findByStatus(
                PostStatus.SCHEDULED);
    }
}