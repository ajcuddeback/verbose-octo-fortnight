package com.budgetapp.dto.request;

import com.budgetapp.model.BillFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillRequest {

    @NotBlank(message = "Bill name is required")
    private String name;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Due day is required")
    private Integer dueDay;

    @NotNull(message = "Frequency is required")
    private BillFrequency frequency;

    private Long categoryId;

    private boolean isAutoPay;

    private boolean isActive = true;
}
