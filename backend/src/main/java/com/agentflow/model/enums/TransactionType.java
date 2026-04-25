package com.agentflow.model.enums;

public enum TransactionType {
    MONTHLY_GRANT,      // automatic monthly credit top-up
    PURCHASE,           // one-time credit pack purchase via Stripe
    WORKFLOW_DEDUCTION, // credits spent starting a workflow
    MANUAL_ADJUSTMENT,  // admin correction
    REFUND              // credits returned on failed workflow
}
