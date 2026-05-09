package com.hasa.linkedIn.Post.Generator.repository;

import com.hasa.linkedIn.Post.Generator.model.Post;
import com.hasa.linkedIn.Post.Generator.model.PostStatus;
import com.hasa.linkedIn.Post.Generator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStatus(PostStatus status);

    List<Post> findByUser(User user);

    List<Post> findByUserAndStatus(User user, PostStatus status);

    List<Post> findByUserAndStatusNot(User user, PostStatus status);

    List<Post> findByStatusAndScheduledTimeBefore(
            PostStatus status,
            LocalDateTime time
    );
}