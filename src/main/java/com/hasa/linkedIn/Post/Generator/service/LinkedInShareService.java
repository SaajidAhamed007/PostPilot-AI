package com.hasa.linkedIn.Post.Generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hasa.linkedIn.Post.Generator.model.User;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class LinkedInShareService {

    private static final String LINKEDIN_POST_URL =
            "https://api.linkedin.com/v2/ugcPosts";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LinkedInShareService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {

        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean publishToLinkedIn(User user, String content) {

        try {

            if (user.getLinkedinAccessToken() == null ||
                    user.getLinkedinAccessToken().isBlank()) {

                return false;
            }

            Map<String, Object> payload = new HashMap<>();

            payload.put(
                    "author",
                    "urn:li:person:" + user.getLinkedinUserId());

            payload.put("lifecycleState", "PUBLISHED");

            Map<String, Object> commentary = new HashMap<>();
            commentary.put("text", content);

            Map<String, Object> shareContent = new HashMap<>();
            shareContent.put("shareCommentary", commentary);
            shareContent.put("shareMediaCategory", "NONE");

            Map<String, Object> specificContent = new HashMap<>();
            specificContent.put(
                    "com.linkedin.ugc.ShareContent",
                    shareContent);

            payload.put("specificContent", specificContent);

            Map<String, Object> visibility = new HashMap<>();
            visibility.put(
                    "com.linkedin.ugc.MemberNetworkVisibility",
                    "PUBLIC");

            payload.put("visibility", visibility);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setBearerAuth(user.getLinkedinAccessToken());

            HttpEntity<String> entity =
                    new HttpEntity<>(
                            objectMapper.writeValueAsString(payload),
                            headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            LINKEDIN_POST_URL,
                            HttpMethod.POST,
                            entity,
                            String.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {

            return false;
        }
    }
}