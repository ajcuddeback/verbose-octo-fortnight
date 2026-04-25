package com.agentflow.dto;

import com.agentflow.model.enums.SubscriptionPlan;
import com.agentflow.model.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private SubscriptionPlan plan;
    private String planDisplayName;
    private double monthlyPrice;
    private int monthlyCredits;
    private SubscriptionStatus status;
    private LocalDateTime currentPeriodEnd;
}
