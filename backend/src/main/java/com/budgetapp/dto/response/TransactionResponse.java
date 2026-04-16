package com.budgetapp.dto.response;

import com.budgetapp.model.TransactionType;
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
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private TransactionType type;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private Long accountId;
    private String accountName;
    private String notes;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
