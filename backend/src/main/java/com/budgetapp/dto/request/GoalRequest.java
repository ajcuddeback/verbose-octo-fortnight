package com.budgetapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {

    @NotBlank(message = "Goal name is required")
    private String name;

    @NotNull(message = "Target amount is required")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    private LocalDate targetDate;

    private String category;

    private String description;
}
