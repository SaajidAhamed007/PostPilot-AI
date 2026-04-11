package com.hasa.linkedIn.Post.Generator.controller;

import com.hasa.linkedIn.Post.Generator.config.JwtUtil;
import com.hasa.linkedIn.Post.Generator.dto.LoginRequest;
import com.hasa.linkedIn.Post.Generator.dto.SignupRequest;
import com.hasa.linkedIn.Post.Generator.dto.AuthResponse;
import com.hasa.linkedIn.Post.Generator.model.User;
import com.hasa.linkedIn.Post.Generator.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {

        User user = new User();
        user.setName(signupRequest.getName());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(signupRequest.getPassword());

        User createdUser = authService.signup(user);
        String token = jwtUtil.generateToken(createdUser.getId(), createdUser.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AuthResponse.fromUser(createdUser, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {

        User user = new User();
        user.setEmail(loginRequest.getEmail());
        user.setPassword(loginRequest.getPassword());

        User authenticatedUser = authService.login(user);
        String token = jwtUtil.generateToken(authenticatedUser.getId(), authenticatedUser.getEmail());

        return ResponseEntity.ok(AuthResponse.fromUser(authenticatedUser, token));
    }
}