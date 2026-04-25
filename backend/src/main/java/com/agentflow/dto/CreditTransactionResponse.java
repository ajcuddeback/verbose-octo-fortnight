package com.agentflow.dto;

import com.agentflow.model.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreditTransactionResponse {
    private Long id;
    private int amount;
    private int balanceAfter;
    private TransactionType type;
    private String description;
    private Long projectId;
    private LocalDateTime createdAt;
}
