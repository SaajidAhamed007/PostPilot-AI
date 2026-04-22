package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.config.JwtUtil;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

        private final UserRepository userRepository;
        private final JwtUtil jwtUtil;
        private final RestTemplate restTemplate = new RestTemplate();

        @Value("${linkedin.client.id}")
        private String clientId;

        @Value("${linkedin.client.secret}")
        private String clientSecret;

        @Value("${linkedin.redirect.uri}")
        private String redirectUri;

        public AuthController(
                        UserRepository userRepository,
                        JwtUtil jwtUtil) {
                this.userRepository = userRepository;
                this.jwtUtil = jwtUtil;
        }

        /**
         * STEP 1
         * Redirect user to LinkedIn login
         */
        @GetMapping("/auth/linkedin/login")
        public ResponseEntity<Void> login() {

                String scope = URLEncoder.encode(
                                "openid profile email w_member_social",
                                StandardCharsets.UTF_8);

                String url = "https://www.linkedin.com/oauth/v2/authorization"
                                + "?response_type=code"
                                + "&client_id=" + clientId
                                + "&redirect_uri=" + redirectUri
                                + "&scope=" + scope;

                return ResponseEntity
                                .status(HttpStatus.FOUND)
                                .header("Location", url)
                                .build();
        }

        /**
         * STEP 2
         * LinkedIn redirects here with authorization code
         */
        @GetMapping("/login/oauth2/code/linkedin")
        public ResponseEntity<?> callback(@RequestParam String code) {

                // Exchange code → access token
                String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken"
                                + "?grant_type=authorization_code"
                                + "&code=" + code
                                + "&redirect_uri=" + redirectUri
                                + "&client_id=" + clientId
                                + "&client_secret=" + clientSecret;

                ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                                tokenUrl,
                                null,
                                Map.class);

                String accessToken = (String) tokenResponse.getBody().get("access_token");

                // Fetch LinkedIn profile
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);

                HttpEntity<?> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> profileResponse = restTemplate.exchange(
                                "https://api.linkedin.com/v2/userinfo",
                                HttpMethod.GET,
                                entity,
                                Map.class);

                Map profile = profileResponse.getBody();

                String linkedinId = (String) profile.get("sub");
                String email = (String) profile.get("email");
                String name = (String) profile.get("name");

                // Save or update user
                Optional<User> optionalUser = userRepository.findByEmail(email);

                User user;

                if (optionalUser.isPresent()) {

                        user = optionalUser.get();

                } else {

                        user = new User();
                        user.setEmail(email);
                        user.setName(name);
                }

                user.setLinkedinUserId(linkedinId);
                user.setLinkedinAccessToken(accessToken);
                user.setLinkedinConnected(true);

                userRepository.save(user);

                // Generate JWT
                String jwt = jwtUtil.generateToken(
                                user.getId(),
                                user.getEmail());

                // Redirect to frontend with token and LinkedIn user ID
                String frontendCallbackUrl = "http://localhost:5173/auth/callback"
                                + "?token=" + jwt
                                + "&linkedinUserId=" + linkedinId;

                return ResponseEntity
                                .status(HttpStatus.FOUND)
                                .header("Location", frontendCallbackUrl)
                                .build();
        }

        /**
         * STEP 3
         * Get current user profile using JWT token
         */
        @GetMapping("/auth/me")
        public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
                try {
                        // Extract JWT from Authorization header
                        String token = authHeader.replace("Bearer ", "");

                        // Validate token
                        if (!jwtUtil.validateToken(token)) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("error", "Invalid token"));
                        }

                        // Get user ID from token
                        Long userId = jwtUtil.getUserIdFromToken(token);

                        // Fetch user from database
                        Optional<User> optionalUser = userRepository.findById(userId);

                        if (optionalUser.isEmpty()) {
                                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(Map.of("error", "User not found"));
                        }

                        User user = optionalUser.get();

                        // Return user info (excluding sensitive data)
                        return ResponseEntity.ok(
                                        Map.of(
                                                "id", user.getId(),
                                                "name", user.getName(),
                                                "email", user.getEmail(),
                                                "linkedinConnected", user.isLinkedinConnected()
                                        )
                        );

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", e.getMessage()));
                }
        }
}