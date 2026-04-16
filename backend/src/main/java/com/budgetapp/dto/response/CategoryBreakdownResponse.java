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
public class CategoryBreakdownResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryData {
        private Long categoryId;
        private String categoryName;
        private String colorHex;
        private BigDecimal totalAmount;
        private double percentage;
    }

    private List<CategoryData> categories;
    private BigDecimal totalAmount;
}
