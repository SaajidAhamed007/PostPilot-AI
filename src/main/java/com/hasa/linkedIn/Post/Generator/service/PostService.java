package com.hasa.linkedIn.Post.Generator.service;

import com.hasa.linkedIn.Post.Generator.integration.GeminiClient;
import com.hasa.linkedIn.Post.Generator.kafka.KafkaPostProducer;
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
    private final KafkaPostProducer kafkaPostProducer;
    private final LinkedInShareService linkedInShareService;

    public PostService(
            PostRepository postRepository,
            GeminiClient geminiClient,
            KafkaPostProducer kafkaPostProducer,
            LinkedInShareService linkedInShareService
    ) {

        this.postRepository = postRepository;
        this.geminiClient = geminiClient;
        this.kafkaPostProducer = kafkaPostProducer;
        this.linkedInShareService = linkedInShareService;
    }

    // =========================
    // CREATE DRAFT
    // =========================

    public Post createDraftForUser(
            Post post,
            User user
    ) {

        post.setStatus(PostStatus.DRAFT);

        post.setCreatedAt(LocalDateTime.now());

        post.setUser(user);

        return postRepository.save(post);
    }

    // =========================
    // PUBLISH NOW
    // =========================

    public Post publishNow(
            Post post,
            User user
    ) {

        post.setStatus(PostStatus.QUEUED);

        post.setCreatedAt(LocalDateTime.now());

        post.setUser(user);

        Post savedPost =
                postRepository.save(post);

        kafkaPostProducer.publish(
                savedPost.getId()
        );

        return savedPost;
    }

public void processPostPublishing(
        Long postId
) {

    Post post =
            postRepository.findById(postId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Post not found"
                            ));

    post.setStatus(
            PostStatus.PUBLISHING
    );

    postRepository.save(post);

    String fullContent =
            post.getContent();

    if (post.getHashtags() != null &&
            !post.getHashtags().isBlank()) {

        fullContent += "\n\n" +
                post.getHashtags();
    }

    boolean success =
            linkedInShareService.publishToLinkedIn(
                    post.getUser(),
                    fullContent
            );

    if (success) {

        post.setStatus(
                PostStatus.PUBLISHED
        );

        post.setPublishedAt(
                LocalDateTime.now()
        );

    } else {

        post.setStatus(
                PostStatus.FAILED
        );
    }

    postRepository.save(post);
}

    // =========================
    // AI GENERATE POST
    // =========================

    public Post generatePost(
            String prompt,
            String existingContent
    ) {

        String aiResponse =
                geminiClient.generatePost(
                        prompt,
                        existingContent
                );

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

            content =
                    contentParts[0].trim();

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

    public List<Post> getPostsForUser(
            User user
    ) {

        return postRepository.findByUser(user);
    }

    // =========================
    // GET POST BY ID
    // =========================

    public Post getPostByIdForUser(
            Long id,
            User user
    ) {

        Post post =
                postRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Post not found"
                                ));

        if (post.getUser() == null ||
                !post.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new RuntimeException(
                    "Unauthorized access"
            );
        }

        return post;
    }

    // =========================
    // UPDATE POST
    // =========================

    public Post updatePost(
            Long id,
            Post updatedPost,
            User user
    ) {

        Post existingPost =
                getPostByIdForUser(id, user);

        if (existingPost.getStatus() ==
                PostStatus.PUBLISHED) {

            throw new RuntimeException(
                    "Published posts cannot be edited"
            );
        }

        existingPost.setTitle(
                updatedPost.getTitle()
        );

        existingPost.setContent(
                updatedPost.getContent()
        );

        existingPost.setHashtags(
                updatedPost.getHashtags()
        );

        existingPost.setImageUrl(
                updatedPost.getImageUrl()
        );

        return postRepository.save(existingPost);
    }

    // =========================
    // DELETE POST
    // =========================

    public void deletePost(
            Long id,
            User user
    ) {

        Post existingPost =
                getPostByIdForUser(id, user);

        if (existingPost.getStatus() ==
                PostStatus.PUBLISHED) {

            throw new RuntimeException(
                    "Published posts cannot be deleted"
            );
        }

        postRepository.delete(existingPost);
    }

    // =========================
    // SCHEDULE POST
    // =========================

    public Post schedulePost(
            Long id,
            LocalDateTime scheduledTime,
            User user
    ) {

        Post post =
                getPostByIdForUser(id, user);

        if (post.getStatus() ==
                PostStatus.PUBLISHED) {

            throw new RuntimeException(
                    "Published posts cannot be scheduled"
            );
        }

        post.setScheduledTime(
                scheduledTime
        );

        post.setStatus(
                PostStatus.SCHEDULED
        );

        return postRepository.save(post);
    }

    // =========================
    // GET SCHEDULED POSTS
    // =========================

    public List<Post> getScheduledPosts() {

        return postRepository.findByStatus(
                PostStatus.SCHEDULED
        );
    }
}
