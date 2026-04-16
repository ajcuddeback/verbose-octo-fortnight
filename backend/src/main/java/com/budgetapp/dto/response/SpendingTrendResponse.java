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
public class SpendingTrendResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDataPoint {
        private int month;
        private int year;
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal netSavings;
    }

    private List<MonthlyDataPoint> dataPoints;
}
