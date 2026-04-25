package com.agentflow.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlan {
    FREE("Free", 0.00, 50),
    STARTER("Starter", 9.99, 500),
    PRO("Pro", 29.99, 2000);

    private final String displayName;
    private final double monthlyPrice;
    private final int monthlyCredits;
}
