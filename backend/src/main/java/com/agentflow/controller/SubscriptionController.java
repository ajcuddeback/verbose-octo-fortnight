package com.agentflow.controller;

import com.agentflow.dto.CheckoutResponse;
import com.agentflow.dto.SubscriptionResponse;
import com.agentflow.model.User;
import com.agentflow.model.enums.SubscriptionPlan;
import com.agentflow.repository.UserRepository;
import com.agentflow.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @GetMapping
    public SubscriptionResponse getSubscription(@AuthenticationPrincipal UserDetails principal) {
        return subscriptionService.getSubscription(resolveUser(principal));
    }

    /** Initiates a subscription upgrade. Returns a Stripe Checkout URL to redirect the user to. */
    @PostMapping("/checkout/{plan}")
    public CheckoutResponse checkout(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable String plan) {
        SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(plan.toUpperCase());
        return subscriptionService.createCheckoutSession(resolveUser(principal), targetPlan);
    }

    /** Initiates a one-time credit pack purchase. Pack keys: PACK_100, PACK_500, PACK_1000 */
    @PostMapping("/credits/checkout/{packKey}")
    public CheckoutResponse creditPackCheckout(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable String packKey) {
        return subscriptionService.createCreditPackCheckout(resolveUser(principal), packKey.toUpperCase());
    }

    @PostMapping("/cancel")
    public SubscriptionResponse cancel(@AuthenticationPrincipal UserDetails principal) {
        return subscriptionService.cancelSubscription(resolveUser(principal));
    }

    /**
     * Stripe webhook endpoint — receives payment events.
     * TODO: Verify the Stripe-Signature header before processing.
     * See SubscriptionService.handleWebhookEvent for the full implementation notes.
     */
    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload,
                        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        log.info("TODO: Parse and verify Stripe webhook payload (signature: {})", signature);
        // TODO: Parse event = Webhook.constructEvent(payload, signature, webhookSecret)
        // Then route to subscriptionService.handleWebhookEvent(...)
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
            .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getUsername()));
    }
}
