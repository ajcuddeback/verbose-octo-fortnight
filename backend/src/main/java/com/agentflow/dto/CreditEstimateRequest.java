package com.agentflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreditEstimateRequest {

    @NotBlank
    @Size(min = 10, max = 5000)
    private String productIdea;
}
