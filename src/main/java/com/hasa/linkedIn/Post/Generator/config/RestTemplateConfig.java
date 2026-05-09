package com.hasa.linkedIn.Post.Generator.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
        RestTemplateBuilder builder
    ) {
        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {

        ObjectMapper mapper =
            new ObjectMapper();

        mapper.registerModule(
            new JavaTimeModule()
        );

        return mapper;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}