package com.budgetapp.dto.response;

import com.budgetapp.model.BillFrequency;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponse {

    private Long id;
    private String name;
    private BigDecimal amount;
    private int dueDay;
    private BillFrequency frequency;
    private Long categoryId;
    private String categoryName;
    @JsonProperty("isAutoPay")
    private boolean isAutoPay;
    @JsonProperty("isActive")
    private boolean isActive;
    private LocalDate lastPaidDate;
    private LocalDate nextDueDate;
    private LocalDateTime createdAt;
}
