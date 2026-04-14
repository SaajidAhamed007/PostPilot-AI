package com.hasa.linkedIn.Post.Generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GeneratePostRequest {

    @NotBlank(message = "Prompt is required")
    @Size(min = 5, max = 1000, message = "Prompt must be between 5 and 1000 characters")
    private String prompt;

    @Size(max = 5000, message = "Existing content must be less than 5000 characters")
    private String existingContent;

    // Constructors
    public GeneratePostRequest() {
    }

    public GeneratePostRequest(String prompt) {
        this.prompt = prompt;
    }

    public GeneratePostRequest(String prompt, String existingContent) {
        this.prompt = prompt;
        this.existingContent = existingContent;
    }

    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getExistingContent() {
        return existingContent;
    }

    public void setExistingContent(String existingContent) {
        this.existingContent = existingContent;
    }
}
