package com.budgetapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private int month;
    private int year;
    private double percentage;
    private LocalDateTime createdAt;
}
