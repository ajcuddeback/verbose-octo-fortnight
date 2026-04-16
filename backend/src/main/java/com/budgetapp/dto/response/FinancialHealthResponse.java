package com.budgetapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealthResponse {

    private Long id;
    private double overallScore;
    private double savingsRateScore;
    private double budgetAdherenceScore;
    private double goalProgressScore;
    private double emergencyFundScore;
    private double consistencyScore;
    private LocalDateTime calculatedAt;
    private Map<String, Double> breakdown;
    private String grade;
    private List<String> insights;
}
