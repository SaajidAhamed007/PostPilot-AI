package com.hasa.linkedIn.Post.Generator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.oauth2SuccessHandler = oauth2SuccessHandler;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> {
                                }) // Enable CORS with defaults from CorsConfig
                                .sessionManagement(session -> session
                                                // OAuth2 needs stateful sessions, so we allow sessions for OAuth
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/auth/**").permitAll() // Allow auth endpoints without
                                                                                         // auth
                                                .requestMatchers("/oauth2/**").permitAll() // Allow OAuth endpoints
                                                .requestMatchers("/login/oauth2/**").permitAll() // Allow OAuth login
                                                .requestMatchers("OPTIONS", "/**").permitAll() // Allow all CORS
                                                                                               // preflight requests
                                                .requestMatchers("/api/posts/**").authenticated()
                                                .anyRequest().permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(endpoint -> endpoint
                                                                .baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(endpoint -> endpoint
                                                                .baseUri("/login/oauth2/code/*"))
                                                .successHandler(oauth2SuccessHandler))
                                .httpBasic(basic -> basic.disable()) // Disable popup
                                .formLogin(form -> form.disable()) // Disable form login
                                .logout(logout -> logout.disable())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
