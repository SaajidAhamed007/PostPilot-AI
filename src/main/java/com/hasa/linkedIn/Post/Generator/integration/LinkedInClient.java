package com.hasa.linkedIn.Post.Generator.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
public class LinkedInClient {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // LinkedIn API endpoints
    private static final String LINKEDIN_API_BASE = "https://api.linkedin.com/v2";
    private static final String LINKEDIN_OAUTH_TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String LINKEDIN_USER_INFO_URL = LINKEDIN_API_BASE + "/me";
    private static final String LINKEDIN_POSTS_URL = LINKEDIN_API_BASE + "/ugcPosts";

    public LinkedInClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get user's LinkedIn profile information
     */
    public Map<String, Object> getUserProfile(String accessToken) {
        try {
            logger.info("Fetching LinkedIn user profile");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String response = restTemplate.getForObject(LINKEDIN_USER_INFO_URL, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            Map<String, Object> profileInfo = new HashMap<>();
            profileInfo.put("id", jsonNode.get("id").asText());
            profileInfo.put("localizedFirstName", jsonNode.path("localizedFirstName").asText());
            profileInfo.put("localizedLastName", jsonNode.path("localizedLastName").asText());

            logger.info("Successfully retrieved LinkedIn profile");
            return profileInfo;

        } catch (Exception e) {
            logger.error("Error fetching LinkedIn user profile", e);
            throw new RuntimeException("Failed to fetch LinkedIn profile: " + e.getMessage());
        }
    }

    /**
     * Publish a post to LinkedIn
     */
    public Map<String, Object> publishPost(String accessToken, String title, String content) {
        try {
            logger.info("Publishing post to LinkedIn");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construct the post payload
            Map<String, Object> postPayload = new HashMap<>();
            postPayload.put("author", "urn:li:person:PERSON_ID"); // This should be set dynamically
            postPayload.put("lifecycleState", "PUBLISHED");

            // Build specific content
            Map<String, Object> specificContent = new HashMap<>();
            specificContent.put("title", title);
            specificContent.put("media", new java.util.ArrayList<>());

            Map<String, Object> commentary = new HashMap<>();
            commentary.put("text", content);

            postPayload.put("specificContent", new HashMap<String, Object>() {
                {
                    put("com.linkedin.ugc.PublishedContent", specificContent);
                }
            });
            postPayload.put("commentary", commentary);
            postPayload.put("visibility", new HashMap<String, Object>() {
                {
                    put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");
                }
            });

            String payloadJson = objectMapper.writeValueAsString(postPayload);
            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

            String response = restTemplate.postForObject(LINKEDIN_POSTS_URL, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            Map<String, Object> result = new HashMap<>();
            result.put("linkedinPostId", jsonNode.get("id").asText());
            result.put("postUrl", "https://www.linkedin.com/feed/update/" + jsonNode.get("id").asText());

            logger.info("Successfully published post to LinkedIn");
            return result;

        } catch (Exception e) {
            logger.error("Error publishing post to LinkedIn", e);
            throw new RuntimeException("Failed to publish post to LinkedIn: " + e.getMessage());
        }
    }

    /**
     * Refresh expired LinkedIn access token
     */
    public Map<String, Object> refreshAccessToken(String clientId, String clientSecret, String refreshToken) {
        try {
            logger.info("Refreshing LinkedIn access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=refresh_token&refresh_token=" + refreshToken +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(LINKEDIN_OAUTH_TOKEN_URL, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", jsonNode.get("access_token").asText());
            result.put("expiresIn", jsonNode.get("expires_in").asInt());
            result.put("tokenType", jsonNode.get("token_type").asText());

            if (jsonNode.has("refresh_token")) {
                result.put("refreshToken", jsonNode.get("refresh_token").asText());
            }

            logger.info("Successfully refreshed LinkedIn access token");
            return result;

        } catch (Exception e) {
            logger.error("Error refreshing LinkedIn access token", e);
            throw new RuntimeException("Failed to refresh LinkedIn token: " + e.getMessage());
        }
    }

    /**
     * Verify if the access token is valid
     */
    public boolean isTokenValid(String accessToken) {
        try {
            logger.info("Verifying LinkedIn access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String response = restTemplate.getForObject(LINKEDIN_USER_INFO_URL, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            boolean isValid = jsonNode.has("id");
            logger.info("Token verification result: {}", isValid);
            return isValid;

        } catch (Exception e) {
            logger.error("Error verifying LinkedIn access token", e);
            return false;
        }
    }
}
