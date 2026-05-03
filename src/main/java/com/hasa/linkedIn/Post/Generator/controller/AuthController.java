package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.config.JwtUtil;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController {

        private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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

        @GetMapping("/auth/linkedin/login")
        public ResponseEntity<Void> login() {
                String scope = URLEncoder.encode(
                                "openid profile email",
                                StandardCharsets.UTF_8);

                String url = "https://www.linkedin.com/oauth/v2/authorization"
                                + "?response_type=code"
                                + "&client_id=" + clientId
                                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                                + "&scope=" + scope;

                return ResponseEntity
                                .status(HttpStatus.FOUND)
                                .header("Location", url)
                                .build();
        }

        @PostMapping("/auth/linkedin")
        public ResponseEntity<?> handleLinkedInCallback(@RequestBody Map<String, String> request) {
                try {
                        String code = request.get("code");

                        if (code == null || code.trim().isEmpty()) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Authorization code is required"));
                        }

                        logger.info("Received authorization code: {}",
                                        code.substring(0, Math.min(20, code.length())) + "...");

                        // Exchange authorization code for access token
                        // LinkedIn requires form-encoded body, not query parameters
                        String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

                        logger.info("Calling LinkedIn token endpoint: {}", tokenUrl);
                        logger.info("Using Redirect URI: {}", redirectUri);
                        logger.info("Client ID: {}", clientId);

                        // Prepare form-encoded body
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                        String body = "grant_type=authorization_code"
                                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                                        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

                        HttpEntity<String> entity = new HttpEntity<>(body, headers);

                        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                                        tokenUrl,
                                        entity,
                                        Map.class);

                        logger.info("LinkedIn token response status: {}", tokenResponse.getStatusCode());
                        logger.debug("LinkedIn token response body: {}", tokenResponse.getBody());

                        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                                logger.error("Failed to get access token from LinkedIn. Status: {}, Body: {}",
                                                tokenResponse.getStatusCode(), tokenResponse.getBody());
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Failed to exchange authorization code for access token",
                                                "details", tokenResponse.getBody()));
                        }

                        Map<String, Object> tokenBody = tokenResponse.getBody();
                        String accessToken = (String) tokenBody.get("access_token");

                        if (accessToken == null || accessToken.trim().isEmpty()) {
                                logger.error("No access token in LinkedIn response: {}", tokenBody);
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "No access token received from LinkedIn"));
                        }

                        logger.info("Successfully obtained access token from LinkedIn");

                        // Fetch user profile using the access token
                        HttpHeaders profileHeaders = new HttpHeaders();
                        profileHeaders.setBearerAuth(accessToken);
                        HttpEntity<?> profileEntity = new HttpEntity<>(profileHeaders);

                        logger.info("Fetching user profile from LinkedIn API");

                        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                                        "https://api.linkedin.com/v2/userinfo",
                                        HttpMethod.GET,
                                        profileEntity,
                                        Map.class);

                        if (!profileResponse.getStatusCode().is2xxSuccessful() || profileResponse.getBody() == null) {
                                logger.error("Failed to fetch LinkedIn profile. Status: {}",
                                                profileResponse.getStatusCode());
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Failed to fetch LinkedIn user profile"));
                        }

                        Map<String, Object> profile = profileResponse.getBody();

                        // Extract user information from LinkedIn profile
                        String linkedinId = (String) profile.get("sub");
                        String email = (String) profile.get("email");
                        String name = (String) profile.get("name");
                        String picture = (String) profile.getOrDefault("picture", null);

                        if (linkedinId == null || email == null) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "error", "Invalid LinkedIn profile data"));
                        }

                        // Check if user exists in database
                        Optional<User> optionalUser = userRepository.findByEmail(email);
                        User user;

                        if (optionalUser.isPresent()) {
                                // Update existing user
                                user = optionalUser.get();
                                user.setLinkedinUserId(linkedinId);
                                user.setLinkedinConnected(true);
                                if (picture != null) {
                                        user.setProfilePicture(picture);
                                }
                        } else {
                                // Create new user
                                user = new User();
                                user.setEmail(email);
                                user.setName(name);
                                user.setLinkedinUserId(linkedinId);
                                user.setLinkedinConnected(true);
                                user.setProfilePicture(picture);
                                // Set a default password for LinkedIn OAuth users
                                user.setPassword(""); // Empty password indicates OAuth-only user
                        }

                        // Save user to database with LinkedIn token
                        user.setLinkedinAccessToken(accessToken);
                        userRepository.save(user);

                        logger.info("User saved/updated successfully with LinkedIn token. ID: {}", user.getId());

                        // Return response with LinkedIn access token (for frontend verification)
                        Map<String, Object> response = new HashMap<>();
                        response.put("token", accessToken); // LinkedIn token for frontend verification
                        response.put("user", Map.of(
                                        "id", user.getId(),
                                        "name", user.getName(),
                                        "email", user.getEmail(),
                                        "profilePicture", user.getProfilePicture(),
                                        "linkedinId", user.getLinkedinUserId()));

                        logger.info("Successfully authenticated user: {}", user.getEmail());
                        return ResponseEntity.ok(response);

                } catch (RestClientException e) {
                        logger.error("RestClientException while communicating with LinkedIn", e);
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                                        "error", "Failed to communicate with LinkedIn OAuth servers",
                                        "details", e.getMessage()));
                } catch (Exception e) {
                        logger.error("Unexpected error during LinkedIn OAuth exchange", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                                        "error", "An unexpected error occurred during authentication",
                                        "details", e.getMessage()));
                }
        }

        @GetMapping("/auth/debug")
        public ResponseEntity<?> debugConfig() {
                logger.info("=== LinkedIn OAuth Configuration Debug ===");
                logger.info("Client ID configured: {}", clientId != null && !clientId.isEmpty());
                logger.info("Client Secret configured: {}", clientSecret != null && !clientSecret.isEmpty());
                logger.info("Redirect URI: {}", redirectUri);

                return ResponseEntity.ok(Map.of(
                                "clientIdConfigured", clientId != null && !clientId.isEmpty(),
                                "clientSecretConfigured", clientSecret != null && !clientSecret.isEmpty(),
                                "redirectUri", redirectUri,
                                "status", "Check backend logs for more details"));
        }

        @GetMapping("/login/oauth2/code/linkedin")
        public ResponseEntity<?> legacyCallback(@RequestParam String code) {
                try {
                        logger.info("LinkedIn OAuth callback received with code: {}",
                                        code.substring(0, Math.min(20, code.length())) + "...");

                        // Exchange authorization code for access token
                        String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                        String body = "grant_type=authorization_code"
                                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                                        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

                        HttpEntity<String> entity = new HttpEntity<>(body, headers);

                        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                                        tokenUrl,
                                        entity,
                                        Map.class);

                        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                                logger.error("Failed to get access token. Status: {}", tokenResponse.getStatusCode());
                                return ResponseEntity
                                                .status(HttpStatus.FOUND)
                                                .header("Location",
                                                                "http://localhost:5173/oauth-callback?error=token_exchange_failed")
                                                .build();
                        }

                        Map<String, Object> tokenBody = tokenResponse.getBody();
                        String accessToken = (String) tokenBody.get("access_token");

                        if (accessToken == null || accessToken.trim().isEmpty()) {
                                logger.error("No access token in response");
                                return ResponseEntity
                                                .status(HttpStatus.FOUND)
                                                .header("Location",
                                                                "http://localhost:5173/oauth-callback?error=no_access_token")
                                                .build();
                        }

                        // Fetch user profile using the access token
                        HttpHeaders profileHeaders = new HttpHeaders();
                        profileHeaders.setBearerAuth(accessToken);
                        HttpEntity<?> profileEntity = new HttpEntity<>(profileHeaders);

                        ResponseEntity<Map> profileResponse = restTemplate.exchange(
                                        "https://api.linkedin.com/v2/userinfo",
                                        HttpMethod.GET,
                                        profileEntity,
                                        Map.class);

                        if (!profileResponse.getStatusCode().is2xxSuccessful() || profileResponse.getBody() == null) {
                                logger.error("Failed to fetch profile. Status: {}", profileResponse.getStatusCode());
                                return ResponseEntity
                                                .status(HttpStatus.FOUND)
                                                .header("Location",
                                                                "http://localhost:5173/oauth-callback?error=profile_fetch_failed")
                                                .build();
                        }

                        Map<String, Object> profile = profileResponse.getBody();
                        String linkedinId = (String) profile.get("sub");
                        String email = (String) profile.get("email");
                        String name = (String) profile.get("name");
                        String picture = (String) profile.getOrDefault("picture", null);

                        if (linkedinId == null || email == null) {
                                logger.error("Invalid profile data from LinkedIn");
                                return ResponseEntity
                                                .status(HttpStatus.FOUND)
                                                .header("Location",
                                                                "http://localhost:5173/oauth-callback?error=invalid_profile")
                                                .build();
                        }

                        // Check if user exists in database
                        Optional<User> optionalUser = userRepository.findByEmail(email);
                        User user;

                        if (optionalUser.isPresent()) {
                                user = optionalUser.get();
                                user.setLinkedinUserId(linkedinId);
                                user.setLinkedinConnected(true);
                                if (picture != null) {
                                        user.setProfilePicture(picture);
                                }
                        } else {
                                user = new User();
                                user.setEmail(email);
                                user.setName(name);
                                user.setLinkedinUserId(linkedinId);
                                user.setLinkedinConnected(true);
                                user.setProfilePicture(picture);
                                user.setPassword("");
                        }

                        // Save user with LinkedIn token
                        user.setLinkedinAccessToken(accessToken);
                        userRepository.save(user);

                        logger.info("User authenticated: {}", email);

                        // Redirect to frontend with token in query parameter
                        String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail());

                        String frontendCallbackUrl =
                        "http://localhost:5173/oauth-callback?token=" +
                        URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);

                        return ResponseEntity
                                .status(HttpStatus.FOUND)
                                .header("Location", frontendCallbackUrl)
                                .build();
                } catch (Exception e) {
                        logger.error("OAuth callback error", e);
                        return ResponseEntity
                                        .status(HttpStatus.FOUND)
                                        .header("Location", "http://localhost:5173/oauth-callback?error=" +
                                                        URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8))
                                        .build();
                }
        }

        @GetMapping("/auth/me")
                public ResponseEntity<?> getCurrentUser(
                        @RequestHeader(value = "Authorization", required = false) String authHeader) {

                try {
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                                "error", "Missing or invalid authorization header"));
                        }

                        String token = authHeader.substring(7);

                        // ✅ Validate JWT
                        if (!jwtUtil.validateToken(token)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                                "error", "Invalid or expired token"));
                        }

                        // ✅ Extract user
                        Long userId = jwtUtil.extractUserId(token);

                        Optional<User> userOptional = userRepository.findById(userId);
                        if (userOptional.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                "error", "User not found"));
                        }

                        User user = userOptional.get();

                        return ResponseEntity.ok(Map.of(
                                "id", user.getId(),
                                "name", user.getName(),
                                "email", user.getEmail(),
                                "profilePicture", user.getProfilePicture(),
                                "linkedinId", user.getLinkedinUserId()
                        ));

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                                "error", "Token verification failed",
                                "details", e.getMessage()));
                }
        }
}