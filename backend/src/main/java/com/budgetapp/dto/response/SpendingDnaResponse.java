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
public class SpendingDnaResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategory {
        private String categoryName;
        private BigDecimal totalAmount;
        private double percentage;
    }

    private String personalityType;
    private String personalityDescription;
    private List<TopCategory> topCategories;
    private List<String> insights;
}
