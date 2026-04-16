package com.budgetapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyPrediction {
        private int month;
        private int year;
        private BigDecimal predictedExpenses;
        private BigDecimal predictedIncome;
    }

    private BigDecimal predictedExpenses;
    private BigDecimal predictedIncome;
    private double confidence;
    private int basedOnMonths;
    private List<MonthlyPrediction> monthlyBreakdown;
}
