package com.agentflow.dto;

import com.agentflow.model.enums.ComplexityTier;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreditEstimateResponse {
    private ComplexityTier tier;
    private int creditCost;
    private String summary;
    private String rationale;
    private int wordCount;
}
