package com.hasa.linkedIn.Post.Generator.service;

import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.PostStatus;
import com.hasa.linkedIn.Post.Generator.repository.PostRepository;
import org.springframework.stereotype.Service;
import com.hasa.linkedIn.Post.Generator.integration.GeminiClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final GeminiClient geminiClient;

    public PostService(PostRepository postRepository, GeminiClient geminiClient) {
        this.postRepository = postRepository;
        this.geminiClient = geminiClient;
    }

    public Post createDraft(Post post) {

        post.setStatus(PostStatus.DRAFT);
        post.setCreatedAt(LocalDateTime.now());

        return postRepository.save(post);
    }

    public Post generatePost(String prompt) {
        return generatePost(prompt, null);
    }

    public Post generatePost(String prompt, String existingContent) {

        String aiResponse = geminiClient.generatePost(prompt, existingContent);

        String title = "";
        String content = "";
        String hashtags = "";

        String[] parts = aiResponse.split("Content:");

        if (parts.length > 1) {

            title = parts[0].replace("Title:", "").trim();

            String[] contentParts = parts[1].split("Hashtags:");

            content = contentParts[0].trim();

            if (contentParts.length > 1) {
                hashtags = contentParts[1].trim();
            }
        }

        Post post = new Post();

        post.setTitle(title);
        post.setContent(content);
        post.setHashtags(hashtags);
        post.setStatus(PostStatus.DRAFT);
        post.setCreatedAt(LocalDateTime.now());

        return post;
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public Post schedulePost(Long postId, LocalDateTime scheduledTime) {

        Optional<Post> optionalPost = postRepository.findById(postId);

        if (optionalPost.isPresent()) {

            Post post = optionalPost.get();

            post.setScheduledTime(scheduledTime);
            post.setStatus(PostStatus.SCHEDULED);

            return postRepository.save(post);
        }

        return null;
    }

    public List<Post> getScheduledPosts() {
        return postRepository.findByStatus(PostStatus.SCHEDULED);
    }

    public Post updateStatus(Post post, PostStatus status) {

        post.setStatus(status);

        return postRepository.save(post);
    }

    public Post updatePost(Post post) {
        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}