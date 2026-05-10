package com.hasa.linkedIn.Post.Generator.dto;

import com.hasa.linkedIn.Post.Generator.model.Post;
import java.time.LocalDateTime;

public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String hashtags;
    private String imageUrl;
    private String status;
    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private Long userId;
    private String userName;

    // Constructors
    public PostResponse() {
    }

    public PostResponse(Long id, String title, String content, String hashtags, String imageUrl,
            String status, LocalDateTime scheduledTime, LocalDateTime createdAt,
            LocalDateTime publishedAt, Long userId, String userName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.hashtags = hashtags;
        this.imageUrl = imageUrl;
        this.status = status;
        this.scheduledTime = scheduledTime;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.userId = userId;
        this.userName = userName;
    }

    // Factory method to convert Post entity to PostResponse
    public static PostResponse fromPost(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getHashtags(),
                post.getImageUrl(),
                post.getStatus() != null ? post.getStatus().toString() : "DRAFT",
                post.getScheduledTime(),
                post.getCreatedAt(),
                post.getPublishedAt(),
                post.getUser() != null ? post.getUser().getId() : null,
                post.getUser() != null ? post.getUser().getName() : null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
