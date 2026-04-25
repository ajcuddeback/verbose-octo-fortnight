package com.agentflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CreditBalanceResponse {
    private int balance;
    private int monthlyAllowance;
    private LocalDate lastResetDate;
    private LocalDate nextResetDate;
    private String subscriptionPlan;
    private String subscriptionStatus;
}
