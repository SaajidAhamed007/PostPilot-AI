package com.hasa.linkedIn.Post.Generator.service;

import com.hasa.linkedIn.Post.Generator.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle LinkedIn Share API integration.
 * This service uses the LinkedIn Share on LinkedIn API to post content to
 * user's LinkedIn profile.
 * 
 * API Documentation:
 * https://learn.microsoft.com/en-us/linkedin/shared/share-on-linkedin/share-on-linkedin
 */
@Service
public class LinkedInShareService {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInShareService.class);
    private static final String LINKEDIN_API_BASE = "https://api.linkedin.com/v2/ugcPosts";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LinkedInShareService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Shares a post to LinkedIn using the Share API.
     * 
     * @param user        The user with LinkedIn access token
     * @param postContent The content to share
     * @return LinkedInShareResponse with share status
     */
    public LinkedInShareResponse sharePostToLinkedIn(User user, String postContent) {
        try {
            if (user.getLinkedinAccessToken() == null || user.getLinkedinAccessToken().isEmpty()) {
                logger.warn("User {} does not have LinkedIn access token", user.getId());
                return LinkedInShareResponse.failure("LinkedIn account not connected");
            }

            // Check if token is expired
            if (user.getLinkedinTokenExpiry() != null &&
                    LocalDateTime.now().isAfter(user.getLinkedinTokenExpiry())) {
                logger.warn("LinkedIn access token expired for user {}", user.getId());
                return LinkedInShareResponse.failure("LinkedIn token expired. Please reconnect your account");
            }

            logger.info("Sharing post to LinkedIn for user: {}", user.getId());

            // Build request payload
            Map<String, Object> payload = buildSharePayload(user, postContent);

            // Create headers with OAuth token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(user.getLinkedinAccessToken());

            // Make the API request
            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(payload),
                    headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    LINKEDIN_API_BASE,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String shareId = extractShareIdFromResponse(response.getBody());
                logger.info("Post shared successfully to LinkedIn. Share ID: {}", shareId);
                return LinkedInShareResponse.success(shareId);
            } else {
                logger.error("Failed to share post to LinkedIn. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return LinkedInShareResponse.failure(
                        "Failed to share: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            logger.error("REST client error while sharing to LinkedIn", e);
            return LinkedInShareResponse.failure("Failed to connect to LinkedIn API: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while sharing post to LinkedIn", e);
            return LinkedInShareResponse.failure("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Shares a post to LinkedIn with custom distribution parameters.
     * 
     * @param user           The user with LinkedIn access token
     * @param postContent    The content to share
     * @param commentaryText Optional commentary to add
     * @return LinkedInShareResponse with share status
     */
    public LinkedInShareResponse sharePostToLinkedInWithCommentary(
            User user,
            String postContent,
            String commentaryText) {
        try {
            if (user.getLinkedinAccessToken() == null || user.getLinkedinAccessToken().isEmpty()) {
                logger.warn("User {} does not have LinkedIn access token", user.getId());
                return LinkedInShareResponse.failure("LinkedIn account not connected");
            }

            logger.info("Sharing post with commentary to LinkedIn for user: {}", user.getId());

            // Build request payload with commentary
            Map<String, Object> payload = buildSharePayloadWithCommentary(
                    user,
                    postContent,
                    commentaryText);

            // Create headers with OAuth token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(user.getLinkedinAccessToken());

            // Make the API request
            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(payload),
                    headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    LINKEDIN_API_BASE,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String shareId = extractShareIdFromResponse(response.getBody());
                logger.info("Post with commentary shared successfully to LinkedIn. Share ID: {}", shareId);
                return LinkedInShareResponse.success(shareId);
            } else {
                logger.error("Failed to share post with commentary to LinkedIn. Status: {}",
                        response.getStatusCode());
                return LinkedInShareResponse.failure(
                        "Failed to share: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error while sharing post with commentary to LinkedIn", e);
            return LinkedInShareResponse.failure("Error: " + e.getMessage());
        }
    }

    /**
     * Builds the payload for LinkedIn UGC Posts API request.
     * This is the proper format for posting content to LinkedIn using the v2 API.
     * 
     * API Docs:
     * https://learn.microsoft.com/en-us/linkedin/marketing/integrations/community-management/shares/ugc-post-api
     */
    private Map<String, Object> buildSharePayload(User user, String postContent) {
        Map<String, Object> payload = new HashMap<>();

        // Required: Author (user's LinkedIn ID in URN format)
        if (user.getLinkedinUserId() == null || user.getLinkedinUserId().isEmpty()) {
            logger.warn("User {} does not have LinkedIn user ID set", user.getId());
            payload.put("author", "urn:li:person:unknown");
        } else {
            payload.put("author", "urn:li:person:" + user.getLinkedinUserId());
        }

        // Required: Lifecycle state
        payload.put("lifecycleState", "PUBLISHED");

        // Required: Specific content with share commentary
        Map<String, Object> shareContent = new HashMap<>();
        Map<String, Object> commentary = new HashMap<>();
        commentary.put("text", postContent);
        shareContent.put("shareCommentary", commentary);
        shareContent.put("shareMediaCategory", "NONE");

        Map<String, Object> specificContent = new HashMap<>();
        specificContent.put("com.linkedin.ugc.ShareContent", shareContent);
        payload.put("specificContent", specificContent);

        // Required: Visibility - PUBLIC
        Map<String, Object> visibility = new HashMap<>();
        visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");
        payload.put("visibility", visibility);

        logger.debug("Built UGC Post payload for user: {}", user.getId());
        return payload;
    }

    /**
     * Builds the payload for LinkedIn UGC Posts API request with additional
     * commentary.
     * Uses the v2 UGC Posts API format.
     */
    private Map<String, Object> buildSharePayloadWithCommentary(
            User user,
            String postContent,
            String commentaryText) {
        Map<String, Object> payload = new HashMap<>();

        // Required: Author (user's LinkedIn ID in URN format)
        if (user.getLinkedinUserId() == null || user.getLinkedinUserId().isEmpty()) {
            logger.warn("User {} does not have LinkedIn user ID set", user.getId());
            payload.put("author", "urn:li:person:unknown");
        } else {
            payload.put("author", "urn:li:person:" + user.getLinkedinUserId());
        }

        // Required: Lifecycle state
        payload.put("lifecycleState", "PUBLISHED");

        // Required: Specific content with share commentary
        Map<String, Object> shareContent = new HashMap<>();
        Map<String, Object> commentary = new HashMap<>();
        // Combine both the original content and the additional commentary
        String combinedText = postContent;
        if (commentaryText != null && !commentaryText.trim().isEmpty()) {
            combinedText = postContent + "\n\n" + commentaryText;
        }
        commentary.put("text", combinedText);
        shareContent.put("shareCommentary", commentary);
        shareContent.put("shareMediaCategory", "NONE");

        Map<String, Object> specificContent = new HashMap<>();
        specificContent.put("com.linkedin.ugc.ShareContent", shareContent);
        payload.put("specificContent", specificContent);

        // Required: Visibility - PUBLIC
        Map<String, Object> visibility = new HashMap<>();
        visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");
        payload.put("visibility", visibility);

        logger.debug("Built UGC Post payload with commentary for user: {}", user.getId());
        return payload;
    }

    /**
     * Extracts the share ID from the LinkedIn API response.
     */
    private String extractShareIdFromResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.isEmpty()) {
                logger.warn("Empty response body from LinkedIn API");
                return "unknown";
            }

            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Object id = response.get("id");
            return id != null ? id.toString() : "unknown";
        } catch (Exception e) {
            logger.warn("Failed to extract share ID from response: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Inner class to represent LinkedIn Share API response
     */
    public static class LinkedInShareResponse {
        private boolean success;
        private String message;
        private String shareId;

        private LinkedInShareResponse(boolean success, String message, String shareId) {
            this.success = success;
            this.message = message;
            this.shareId = shareId;
        }

        public static LinkedInShareResponse success(String shareId) {
            return new LinkedInShareResponse(true, "Successfully shared to LinkedIn", shareId);
        }

        public static LinkedInShareResponse failure(String message) {
            return new LinkedInShareResponse(false, message, null);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getShareId() {
            return shareId;
        }

        @Override
        public String toString() {
            return "LinkedInShareResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", shareId='" + shareId + '\'' +
                    '}';
        }
    }
}
