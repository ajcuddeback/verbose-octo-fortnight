package com.agentflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutResponse {
    /** Stripe Checkout session URL — redirect the user here to complete payment. */
    private String checkoutUrl;
    private String sessionId;
}
