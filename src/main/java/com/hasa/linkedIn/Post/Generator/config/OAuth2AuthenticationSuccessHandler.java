package com.hasa.linkedIn.Post.Generator.config;

import com.hasa.linkedIn.Post.Generator.dto.AuthResponse;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
            throws IOException, ServletException {

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            logger.info("OAuth2 login attempt - Email: {}, Name: {}", email, name);

            if (email == null || email.trim().isEmpty()) {
                logger.error("Email is null or empty from OAuth2User");
                response.sendRedirect("http://localhost:5173/login?error=email_missing");
                return;
            }

            // Find or create user
            Optional<User> userOptional = userRepository.findByEmail(email);
            User user;

            if (userOptional.isEmpty()) {
                logger.info("Creating new user with email: {}", email);
                user = new User();
                user.setEmail(email);
                user.setName(name != null ? name : email.split("@")[0]);
                user.setPassword(""); // OAuth users don't have password
                user.setRole("USER");
                user = userRepository.save(user);
                logger.info("User created successfully with ID: {}", user.getId());
            } else {
                logger.info("User already exists with email: {}", email);
                user = userOptional.get();
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getEmail());
            logger.info("JWT token generated for user: {}", user.getId());

            // Create response with token
            AuthResponse authResponse = AuthResponse.fromUser(user, token);

            // URL encode the name to handle special characters
            String encodedName = URLEncoder.encode(user.getName() != null ? user.getName() : "", StandardCharsets.UTF_8);
            String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);

            // Redirect to frontend with token
            String redirectUrl = "http://localhost:5173/auth-callback?token=" + token + 
                    "&userId=" + user.getId() + 
                    "&name=" + encodedName +
                    "&email=" + encodedEmail;

            logger.info("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            logger.error("Error in OAuth2 authentication success handler", e);
            response.sendRedirect("http://localhost:5173/login?error=oauth_failed&message=" + 
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
