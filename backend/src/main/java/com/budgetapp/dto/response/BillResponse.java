package com.budgetapp.dto.response;

import com.budgetapp.model.BillFrequency;
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
    private boolean isAutoPay;
    private boolean isActive;
    private LocalDate lastPaidDate;
    private LocalDate nextDueDate;
    private LocalDateTime createdAt;
}
