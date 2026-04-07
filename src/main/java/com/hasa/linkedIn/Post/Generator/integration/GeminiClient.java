package com.hasa.linkedIn.Post.Generator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generatePost(String prompt) {
        // BUG FIX 1: Correct Gemini API endpoint with proper URL structure
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put(
            "text",
            "Generate a professional LinkedIn post based on this prompt:\n"
                + prompt
                + "\n\nReturn strictly in this format:\n"
                + "Title:\n"
                + "Content:\n"
                + "Hashtags:"
        );

        Map<String, Object> partsWrapper = new HashMap<>();
        partsWrapper.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(partsWrapper));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // BUG FIX 2: Use parameterized Map type to avoid raw type warnings
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response == null || !response.containsKey("candidates")) {
            throw new RuntimeException("Invalid response from Gemini API");
        }

        // BUG FIX 3: Added proper unchecked cast suppression + parameterized types
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

        @SuppressWarnings("unchecked")
        Map<String, Object> firstCandidate = candidates.get(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");

        @SuppressWarnings("unchecked")
        Map<String, Object> textResponse = partsList.get(0);

        return textResponse.get("text").toString();
    }
}