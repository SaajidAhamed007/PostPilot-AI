package com.hasa.linkedIn.Post.Generator.model;

import com.fasterxml.jackson.annotation.JsonIgnore;                 // FIX 4: prevent infinite recursion
import com.fasterxml.jackson.annotation.JsonProperty;              // FIX 3: hide password in responses
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // FIX 3: password will not be returned in API responses
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // FIX 2: default role assigned automatically
    @Column(nullable = false)
    private String role = "USER";

    private LocalDateTime createdAt;

    // FIX 4 + FIX 5:
    // prevents infinite JSON recursion
    // explicitly sets lazy loading
    @JsonIgnore
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<Post> posts;

    // Default constructor
    public User() {}

    // Parameterized constructor
    public User(Long id,
                String name,
                String email,
                String password,
                String role,
                LocalDateTime createdAt,
                List<Post> posts) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
        this.posts = posts;
    }

    // FIX 1: automatically sets createdAt during insert
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ======================
    // GETTERS AND SETTERS
    // ======================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }
}