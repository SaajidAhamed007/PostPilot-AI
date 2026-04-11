package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.config.JwtUtil;
import com.hasa.linkedIn.Post.Generator.dto.AuthResponse;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/oauth2")
public class OAuth2Controller {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public OAuth2Controller(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/callback")
    public ResponseEntity<AuthResponse> oauth2Callback(
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Try to find existing user
            User user = authService.findByEmail(email);

            if (user == null) {
                // Create new user from OAuth2 data
                user = new User();
                user.setEmail(email);
                user.setName(name != null ? name : email.split("@")[0]);
                user.setPassword(""); // OAuth users don't have password
                user.setRole("USER");
                user = authService.createUser(user);
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getEmail());

            return ResponseEntity.ok(AuthResponse.fromUser(user, token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/login/success")
    public ResponseEntity<AuthResponse> oauth2LoginSuccess(
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            OAuth2User oauth2User) {

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Try to find existing user
            User user = authService.findByEmail(email);

            if (user == null) {
                // Create new user from OAuth2 data
                user = new User();
                user.setEmail(email);
                user.setName(name != null ? name : email.split("@")[0]);
                user.setPassword(""); // OAuth users don't have password
                user.setRole("USER");
                user = authService.createUser(user);
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getEmail());

            return ResponseEntity.ok(AuthResponse.fromUser(user, token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
