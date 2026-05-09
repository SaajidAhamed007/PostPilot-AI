package com.hasa.linkedIn.Post.Generator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.hasa.linkedIn.Post.Generator.config.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
        .cors(cors -> {}) // 🔥 ADD THIS
        .csrf(csrf -> csrf.disable())

        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/linkedin/**",
                "/auth/debug",
                "/auth/me",
                "/login/oauth2/code/linkedin",
                "/error",
                "/",
                "/favicon.ico",
                "/api/auth/**"
            ).permitAll()

            // 🔥 ALLOW OPTIONS (CRITICAL)
            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

            .requestMatchers(
                "/api/users/profile/**",
                "/api/users/{id}",
                "/api/posts/**"
            ).authenticated()

            .anyRequest().permitAll()
        )

        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        .formLogin(form -> form.disable())
        .httpBasic(httpBasic -> httpBasic.disable());

    return http.build();
}
}
