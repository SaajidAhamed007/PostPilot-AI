package com.hasa.linkedIn.Post.Generator.dto;

import com.hasa.linkedIn.Post.Generator.model.User;
import java.time.LocalDateTime;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String token;
    private LocalDateTime createdAt;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(Long id, String name, String email, String role, String token, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.token = token;
        this.createdAt = createdAt;
    }

    // Factory method to convert User entity to AuthResponse with token
    public static AuthResponse fromUser(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token,
                user.getCreatedAt());
    }

    // Getters and Setters
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
