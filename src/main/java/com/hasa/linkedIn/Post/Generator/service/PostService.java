package com.hasa.linkedIn.Post.Generator.service;

import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.PostStatus;
import com.hasa.linkedIn.Post.Generator.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post createDraft(Post post) {

        post.setStatus(PostStatus.DRAFT);
        post.setCreatedAt(LocalDateTime.now());

        return postRepository.save(post);
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

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}