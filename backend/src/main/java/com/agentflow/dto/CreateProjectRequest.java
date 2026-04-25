package com.agentflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Product idea is required")
    @Size(min = 20, max = 5000, message = "Please describe your idea in at least 20 characters")
    private String productIdea;
}
