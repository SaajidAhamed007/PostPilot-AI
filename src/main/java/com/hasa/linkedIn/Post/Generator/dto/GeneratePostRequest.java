package com.hasa.linkedIn.Post.Generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GeneratePostRequest {

    @NotBlank(message = "Prompt is required")
    @Size(min = 5, max = 1000, message = "Prompt must be between 5 and 1000 characters")
    private String prompt;

    // Constructors
    public GeneratePostRequest() {
    }

    public GeneratePostRequest(String prompt) {
        this.prompt = prompt;
    }

    // Getters and Setters
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
